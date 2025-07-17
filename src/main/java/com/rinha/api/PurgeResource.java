package com.rinha.api;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/purge")
@ApplicationScoped
public class PurgeResource {
    @Inject
    Redis redis;

    @DELETE
    public Uni<Response> purgeAll() {
        // Limpa as filas principais e resumo (ajuste os nomes conforme usados no seu repositorio)
        return redis.send(io.vertx.mutiny.redis.client.Request.cmd(Command.create("DEL")).arg("payments-queue"))
                .flatMap(r -> redis.send(io.vertx.mutiny.redis.client.Request.cmd(Command.create("DEL")).arg("summary")))
                .map(r -> Response.noContent().build());
    }
}
