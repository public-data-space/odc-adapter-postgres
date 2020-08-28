package de.fraunhofer.fokus.ids.services;

import de.fraunhofer.fokus.ids.messages.DataAssetCreateMessage;
import de.fraunhofer.fokus.ids.persistence.entities.DataSource;
import de.fraunhofer.fokus.ids.persistence.entities.Dataset;
import de.fraunhofer.fokus.ids.persistence.entities.Distribution;
import de.fraunhofer.fokus.ids.persistence.enums.DataAssetStatus;
import de.fraunhofer.fokus.ids.services.sqlite.SQLiteService;
import de.fraunhofer.fokus.ids.services.util.Constants;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

/**
 * @author Vincent Bohlen, vincent.bohlen@fokus.fraunhofer.de
 */
public class DataAssetService {
    private static Logger LOGGER = LoggerFactory.getLogger(DataAssetService.class.getName());

    private SQLiteService sqLiteService;

    public DataAssetService(Vertx vertx){
        sqLiteService = SQLiteService.createProxy(vertx, Constants.SQLITE_SERVICE);
    }

    public void createDataAsset(DataAssetCreateMessage message, Handler<AsyncResult<JsonObject>> resultHandler){
        JsonObject description = message.getData();
        Dataset dataset = new Dataset();
        dataset.setStatus(DataAssetStatus.APPROVED);
        dataset.setDescription(description.getString("datasetdescription", null));
        dataset.setTitle(description.getString("datasettitle", null));
        dataset.setResourceId(UUID.randomUUID().toString());

        DataSource dataSource = message.getDataSource();
        dataset.setSourceId(dataSource.getId());
        Set distributions = new HashSet();
        Distribution dist = new Distribution();
        dist.setFiletype("JSON");
        dist.setFilename(UUID.randomUUID().toString()+".json");
        dist.setResourceId(UUID.randomUUID().toString());
        dist.setDatasetId(dataset.getResourceId());
        distributions.add(dist);
        dataset.setDistributions(distributions);
        Date d = new Date();
        sqLiteService.update("INSERT INTO accessinformation (created_at, updated_at, distributionid, datasetid, query) values(?,?,?,?,?)",
                new JsonArray().add(d.toInstant()).add(d.toInstant())
                        .add(dist.getResourceId())
                        .add(dataset.getResourceId())
                        .add(description.getString("query", "")), reply -> {

            if(reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture(new JsonObject(Json.encode(dataset))));

            } else{
                LOGGER.error("DataAsset info could not be inserted.",reply.cause());
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }

    public void deleteDataAsset(String id, Handler<AsyncResult<JsonObject>> resultHandler){
        sqLiteService.update("DELETE FROM accessinformation WHERE datasetid=?", new JsonArray().add(id), reply -> {
            if(reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            }
            else {
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }
}
