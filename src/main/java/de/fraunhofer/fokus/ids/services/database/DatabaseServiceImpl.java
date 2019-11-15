package de.fraunhofer.fokus.ids.services.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
/**
 * @author Vincent Bohlen, vincent.bohlen@fokus.fraunhofer.de
 */
public class DatabaseServiceImpl implements DatabaseService {
    private Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceImpl.class.getName());
    private Vertx vertx;
    public DatabaseServiceImpl(Vertx vertx, Handler<AsyncResult<DatabaseService>> readyHandler){
        this.vertx = vertx;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public DatabaseService query(JsonObject jdbcConfig, String query, JsonArray params, Handler<AsyncResult<List<JsonObject>>> resultHandler) {
        SQLClient jdbc = PostgreSQLClient.createShared(vertx, jdbcConfig);

        createResult(jdbc, query, params, resultHandler);
        return this;
    }
    /**
     * processing pipeline to create the intended result
     * @param queryString SQL Query to perform
     * @param params Query parameters for the SQL query
     */
    private void createResult(SQLClient jdbcClient, String queryString, JsonArray params, Handler<AsyncResult<List<JsonObject>>> resultHandler){

        createConnection(connection -> handleConnection(connection,
                queryString,
                params,
                result -> handleResult(result,
                        resultHandler
                )),
                jdbcClient);
    }

    /**
     * Method to retrieve the connection from the (postgre) SQL client
     * @param next Handler to perform the query (handleQuery or handleQueryWithParams)
     */
    private void createConnection(Handler<AsyncResult<SQLConnection>> next, SQLClient jdbc){

        jdbc.getConnection(res -> {
            if (res.succeeded()) {
                next.handle(Future.succeededFuture(res.result()));
            }
            else{
                LOGGER.error("Connection could not be established.", res.cause());
                next.handle(Future.failedFuture(res.cause().toString()));
            }
        });
    }

    /**
     * Method to call the correct method specified by the connectionType enum.
     * @param result Connection future produced by createConnection
     * @param queryString SQL String to query
     * @param params params for the SQL query
     * @param next final step of pipeline: handleResult function
     */
    private void handleConnection(AsyncResult<SQLConnection> result,
                                  String queryString,
                                  JsonArray params,
                                  Handler<AsyncResult<List<JsonObject>>> next) {

        handleQuery(result, queryString, params,next);

    }

    /**
     * Method to perform the SQL query on the connection retrieved via createConnection
     * @param result Connection future produced by createConnection
     * @param queryString SQL String to query
     * @param params params for the SQL query
     * @param next final step of pipeline: handleResult function
     */
    private void handleQuery(AsyncResult<SQLConnection> result,
                             String queryString,
                             JsonArray params,
                             Handler<AsyncResult<List<JsonObject>>> next) {

        if(result.failed()){
            LOGGER.error("Connection Future failed.", result.cause());
            next.handle(Future.failedFuture(result.cause()));
        }
        else {
            SQLConnection connection = result.result();
            connection.queryWithParams(queryString, params, query -> {
                if (query.succeeded()) {
                    ResultSet rs = query.result();
                    next.handle(Future.succeededFuture(rs.getRows()));
                    connection.close();
                } else {
                    LOGGER.error("Query failed.", query.cause());
                    next.handle(Future.failedFuture(query.cause()));
                    connection.close();
                }
            });
        }
    }

    /**
     * Process the SQL ResultSet (as List<JSONObject>) and reply the results via receivedMessage
     * @param result SQL ResultSet as List<JsonObject>
     */
    private void handleResult(AsyncResult<List<JsonObject>> result, Handler<AsyncResult<List<JsonObject>>> resultHandler){

        if(result.failed()){
            LOGGER.error("List<JsonObject> Future failed.", result.cause());
            resultHandler.handle(Future.failedFuture(result.cause()));
        }
        else {
            resultHandler.handle(Future.succeededFuture(result.result()));
        }
    }



}
