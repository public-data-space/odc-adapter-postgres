package de.fraunhofer.fokus.ids.services.sqlite;

import de.fraunhofer.fokus.ids.services.database.DatabaseServiceImpl;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;

import java.util.List;

@ProxyGen
@VertxGen
public interface SQLiteService {

    @Fluent
    SQLiteService query(String query, JsonArray params, Handler<AsyncResult<List<JsonObject>>> resultHandler);

    @Fluent
    SQLiteService update(String query, JsonArray params, Handler<AsyncResult<List<JsonObject>>> resultHandler);

    @GenIgnore
    static SQLiteService create(SQLClient dbClient, Handler<AsyncResult<SQLiteService>> readyHandler) {
        return new SQLiteServiceImpl(dbClient, readyHandler);
    }

    @GenIgnore
    static SQLiteService createProxy(Vertx vertx, String address) {
        return new SQLiteServiceVertxEBProxy(vertx, address);
    }
}
