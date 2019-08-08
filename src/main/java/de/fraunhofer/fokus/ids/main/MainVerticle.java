package de.fraunhofer.fokus.ids.main;

import de.fraunhofer.fokus.ids.enums.FileType;
import de.fraunhofer.fokus.ids.messages.DataAssetCreateMessage;
import de.fraunhofer.fokus.ids.messages.ResourceRequest;
import de.fraunhofer.fokus.ids.services.DataAssetService;
import de.fraunhofer.fokus.ids.services.DataService;
import de.fraunhofer.fokus.ids.services.InitService;
import de.fraunhofer.fokus.ids.services.database.DatabaseServiceVerticle;
import de.fraunhofer.fokus.ids.services.sqlite.SQLiteServiceVerticle;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.http.entity.ContentType;

import java.util.*;

    public class MainVerticle extends AbstractVerticle {
        private static Logger LOGGER = LoggerFactory.getLogger(de.fraunhofer.fokus.ids.main.MainVerticle.class.getName());
        private Router router;
        private DataAssetService dataAssetService;
        private DataService dataService;

        @Override
        public void start(Future<Void> startFuture) {

            this.router = Router.router(vertx);
            this.dataAssetService = new DataAssetService(vertx);
            this.dataService = new DataService(vertx);

            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setWorker(true);

            vertx.deployVerticle(SQLiteServiceVerticle.class.getName(), deploymentOptions, reply -> {
                if(reply.succeeded()){
                    LOGGER.info("SQLiteService started");
                    new InitService(vertx, reply2 -> {
                        if(reply.succeeded()){
                            LOGGER.info("Initialization complete.");
                        }
                        else{
                            LOGGER.info("Initialization failed.");
                        }
                    });

                }
                else{
                    LOGGER.info("DataBaseService failed");
                }
            });
            vertx.deployVerticle(DatabaseServiceVerticle.class.getName(), deploymentOptions, reply3 -> {
                if(reply3.succeeded()) {
                    LOGGER.info("DatabaseServiceVerticle started");
                }
                else{
                    LOGGER.info(reply3.cause());
                }
            });

            createHttpServer();
        }

        private void createHttpServer() {
            HttpServer server = vertx.createHttpServer();

            Set<String> allowedHeaders = new HashSet<>();
            allowedHeaders.add("x-requested-with");
            allowedHeaders.add("Access-Control-Allow-Origin");
            allowedHeaders.add("origin");
            allowedHeaders.add("Content-Type");
            allowedHeaders.add("accept");
            allowedHeaders.add("X-PINGARUNER");

            Set<HttpMethod> allowedMethods = new HashSet<>();
            allowedMethods.add(HttpMethod.GET);
            allowedMethods.add(HttpMethod.POST);

            router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
            router.route().handler(BodyHandler.create());

            router.post("/create").handler(routingContext ->
                    dataAssetService.createDataAsset(Json.decodeValue(routingContext.getBodyAsJson().toString(), DataAssetCreateMessage.class), reply ->
                            reply(reply, routingContext.response())));

            router.post("/delete").handler(routingContext ->
                    dataAssetService.deleteDataAsset(Long.parseLong(routingContext.request().getParam("id")), reply ->
                            reply(reply, routingContext.response())));

            router.post("/getFile").handler(routingContext ->
                    dataService.getData(Json.decodeValue(routingContext.getBodyAsString(), ResourceRequest.class), reply ->
                            reply(reply, routingContext.response())));

            router.route("/supported")
                    .handler(routingContext ->
                            supported(result -> reply(result, routingContext.response()))
                    );

            LOGGER.info("Starting Postgres adapter...");
            server.requestHandler(router).listen(8080);
            LOGGER.info("Postgres adapter successfully started.");
        }

        private void supported(Handler<AsyncResult<String>> next) {
            LOGGER.info("Returning supported data formats.");
            JsonArray types = new JsonArray();
            types.add(FileType.JSON);
            types.add(FileType.XML);
            types.add(FileType.TXT);

            JsonObject jO = new JsonObject();
            jO.put("supported", types);

            next.handle(Future.succeededFuture(jO.toString()));
        }

        private void reply(AsyncResult result, HttpServerResponse response) {
            if (result.succeeded()) {
                if (result.result() != null) {
                    String entity = result.result().toString();
                    response.putHeader("content-type", ContentType.APPLICATION_JSON.toString());
                    response.end(entity);
                } else {
                    response.setStatusCode(404).end();
                }
            } else {
                response.setStatusCode(404).end();
            }
        }

        public static void main(String[] args) {
            String[] params = Arrays.copyOf(args, args.length + 1);
            params[params.length - 1] = de.fraunhofer.fokus.ids.main.MainVerticle.class.getName();
            Launcher.executeCommand("run", params);
        }
    }
