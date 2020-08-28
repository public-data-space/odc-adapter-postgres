package de.fraunhofer.fokus.ids.services;

import de.fraunhofer.fokus.ids.services.sqlite.SQLiteService;
import de.fraunhofer.fokus.ids.services.util.Constants;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

/**
 * @author Vincent Bohlen, vincent.bohlen@fokus.fraunhofer.de
 */
public class InitService {
    private final Logger LOGGER = LoggerFactory.getLogger(InitService.class.getName());


    private SQLiteService sqliteService;

    public InitService(Vertx vertx, Handler<AsyncResult<Void>> resultHandler) {
        this.sqliteService = SQLiteService.createProxy(vertx, Constants.SQLITE_SERVICE);

        Future<Void> dbFuture = Future.future();
        initDB(dbFuture.completer());
        Future<Void> configFuture = Future.future();
        register(vertx, configFuture.completer());

        CompositeFuture.all(dbFuture, configFuture).setHandler(reply -> {
            if (reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                LOGGER.error("Table creation failed.", reply.cause());
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }

    private void initDB(Handler<AsyncResult<Void>> resultHandler) {
        sqliteService.update("CREATE TABLE IF NOT EXISTS accessinformation (created_at, updated_at, distributionid, datasetid, query)", new JsonArray(), reply -> {
            if (reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                LOGGER.error("Table creation failed.", reply.cause());
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }

    /**
     * This method is necessary if the adapter is to be started simultaneously with the management components to automatically perform the registration
     *
     * @param vertx         current vertx instance
     * @param resultHandler Future.completer
     */
    private void register(Vertx vertx, Handler<AsyncResult<Void>> resultHandler) {

        ConfigStoreOptions confStore = new ConfigStoreOptions()
                .setType("env");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(confStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {

            if (ar.succeeded()) {
                if (ar.result().containsKey("ROUTE_ALIAS")) {
                    JsonObject registration = new JsonObject()
                            .put("name", ar.result().getString("ROUTE_ALIAS"))
                            .put("address", new JsonObject()
                                    .put("host", ar.result().getString("ROUTE_ALIAS"))
                                    .put("port", 8080));
                    WebClient webClient = WebClient.create(vertx);
                    establishConnection(3, webClient, ar.result().getInteger("CONFIG_MANAGER_PORT"), ar.result().getString("CONFIG_MANAGER_HOST"), "/register", registration, resultHandler);
                }
            } else {
                LOGGER.error(ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void establishConnection(int i, WebClient webClient, int port, String host, String path, JsonObject registration, Handler<AsyncResult<Void>> resultHandler) {
        if (i == 0){
            resultHandler.handle(Future.failedFuture("Connection refused"));
        }
        webClient
                .post(port, host, path)
                .sendJsonObject(registration, ar -> {
            if(ar.succeeded()){
                resultHandler.handle(Future.succeededFuture());
            }
            else{
                establishConnection(i-1,webClient, port, host, path, registration, resultHandler);
            }
        });

    }
}