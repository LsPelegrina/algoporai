package com.rinha.api;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint para limpeza total (purga) do ambiente:
 * Remove fila do Redis e apaga todos os registros do summary/auditoria no Postgres.
 */
@Path("/purge")
@ApplicationScoped
public class PurgeResource {
    @Inject
    Redis redis;
    @Inject
    PgPool pgPool; // Reativo

    @DELETE
    public Uni<Response> purgeAll() {
        // Remove a fila do Redis
        Uni<Void> purgeRedis = redis
                .send(io.vertx.mutiny.redis.client.Request.cmd(Command.DEL).arg("payments-queue"))
                .replaceWithVoid();

        // Limpa locks de idempotência (opcional, se você cria "payment:lock:...")
        // Uni<Void> purgeLocks = redis.send(io.vertx.mutiny.redis.client.Request.cmd(Command.FLUSHDB)).replaceWithVoid();

        // Limpa o summary do Redis (se usava HGETALL de "summary" para algum cache)
        // Uni<Void> purgeSummary = redis.send(io.vertx.mutiny.redis.client.Request.cmd(Command.DEL).arg("summary")).replaceWithVoid();

        // Limpa a tabela de auditoria de pagamentos no Postgres (apaga todos os pagos processados)
        Uni<Void> purgePg = pgPool.query("TRUNCATE TABLE payment_audit").execute().replaceWithVoid();

        // Executa ambas as limpezas em paralelo
        return Uni.combine().all().unis(purgeRedis, purgePg)
                .asTuple()
                .map(tuple -> Response.noContent().build());
    }
}
