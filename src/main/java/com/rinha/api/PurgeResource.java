package com.rinha.api;

import com.rinha.core.RedisRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/admin/purge")
public class PurgeResource {

    @Inject
    RedisRepository redisRepository;

    @DELETE
    public Response purge() {
        redisRepository.purge();
        return Response.ok("Redis data purged.").build();
    }
}