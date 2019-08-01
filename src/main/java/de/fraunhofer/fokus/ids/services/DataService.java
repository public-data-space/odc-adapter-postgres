package de.fraunhofer.fokus.ids.services;

import de.fraunhofer.fokus.ids.messages.ResourceRequest;
import de.fraunhofer.fokus.ids.persistence.entities.DataAsset;
import de.fraunhofer.fokus.ids.persistence.entities.DataSource;
import de.fraunhofer.fokus.ids.services.database.DatabaseService;
import de.fraunhofer.fokus.ids.services.sqlite.SQLiteService;
import de.fraunhofer.fokus.ids.services.util.Constants;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DataService {
    private static Logger LOGGER = LoggerFactory.getLogger(DataService.class.getName());

    private SQLiteService sqLiteService;
    private DatabaseService databaseService;

    public DataService(Vertx vertx){
        this.sqLiteService = SQLiteService.createProxy(vertx, Constants.SQLITE_SERVICE);
        this.databaseService = DatabaseService.createProxy(vertx, Constants.DATABASE_SERVICE);
    }

    public void getData(ResourceRequest request, Handler<AsyncResult<JsonObject>> resultHandler){
        DataSource dataSource = request.getDataSource();
        DataAsset dataAsset = request.getDataAsset();
        sqLiteService.query("SELECT query FROM accessinformation WHERE dataassetid = ?", new JsonArray().add(dataAsset.getId()), reply -> {
           if(reply.succeeded()){
                databaseService.query(dataSource.getData(), reply.result().get(0).getString("query"), new JsonArray(), reply2 -> {
                    if(reply2.succeeded()){

                        JsonObject jO = new JsonObject();
                        jO.put("result", new JsonArray(reply2.result()));
                        resultHandler.handle(Future.succeededFuture(jO));

                    }
                    else{
                        LOGGER.info("Data could not be queries", reply2.cause());
                        resultHandler.handle(Future.failedFuture(reply2.cause()));
                    }
                });
           }
           else{
               LOGGER.info("Query could not be loaded",reply.cause());
               resultHandler.handle(Future.failedFuture(reply.cause()));
           }
        });
    }
}
