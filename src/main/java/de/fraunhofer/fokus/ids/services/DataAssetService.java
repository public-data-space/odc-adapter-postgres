package de.fraunhofer.fokus.ids.services;

import de.fraunhofer.fokus.ids.messages.DataAssetCreateMessage;
import de.fraunhofer.fokus.ids.persistence.entities.DataAsset;
import de.fraunhofer.fokus.ids.persistence.entities.DataSource;
import de.fraunhofer.fokus.ids.persistence.entities.Job;
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

import java.util.Date;
import java.util.Map;
import java.util.UUID;

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

        DataSource dataSource = message.getDataSource();
        DataAsset dataAsset = new DataAsset();
        dataAsset.setId(message.getDataAssetId());
        dataAsset.setSourceID(dataSource.getId());

        JsonObject description = message.getData();

        dataAsset.setFormat(description.getString("format", null));
        dataAsset.setName(description.getString("name", null));
        dataAsset.setResourceID(description.getString("resourceid", null));
        dataAsset.setUrl(description.getString("url", null));
        dataAsset.setOrignalResourceURL(description.getString("originalurl", null));
        dataAsset.setDatasetID(description.getString("datasetId", null));

        dataAsset.setDatasetNotes(description.getString("datasetnotes", null));
        dataAsset.setDatasetTitle(description.getString("datasettitle", null));
        dataAsset.setLicenseTitle(description.getString("licensetitle", null));
        dataAsset.setLicenseUrl(description.getString("licenseurl", null));
        dataAsset.setOrignalDatasetURL(description.getString("originaldataseturl", null));
        dataAsset.setOrganizationDescription(description.getString("organizationdescription", null));
        dataAsset.setOrganizationTitle(description.getString("originalURL", null));
        dataAsset.setTags(description.getJsonArray("tags",new JsonArray()).getList());
        dataAsset.setVersion(description.getString("version", null));
        dataAsset.setDataSetDescription(description.getString("datasetdescription", null));
        dataAsset.setSignature(description.getString("signature", null));
        dataAsset.setStatus(DataAssetStatus.APPROVED);
        dataAsset.setFilename(UUID.randomUUID().toString()+".json");

        Date d = new Date();
        sqLiteService.update("INSERT INTO accessinformation (created_at, updated_at, dataassetid, query) values(?,?,?,?)",
                new JsonArray().add(d.toInstant()).add(d.toInstant())
                        .add(dataAsset.getId())
                        .add(description.getString("query", "")), reply -> {

            if(reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture(new JsonObject(Json.encode(dataAsset))));

            } else{
                LOGGER.error("DataAsset info could not be inserted.",reply.cause());
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }

    public void deleteDataAsset(Long id, Handler<AsyncResult<JsonObject>> resultHandler){
        sqLiteService.update("DELETE FROM accessinformation WHERE dataassetid=?", new JsonArray().add(id), reply -> {
            if(reply.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            }
            else {
                resultHandler.handle(Future.failedFuture(reply.cause()));
            }
        });
    }
}
