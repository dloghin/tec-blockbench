package com.example.analytic.plugin;

import com.example.analytic.api.AnalyticsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.webserver.services.WebServerPluginRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AnalyticsPlugin implements WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(AnalyticsApi::new);

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, String> getStaticServeDirs() { return new HashMap<String, String>(); }
    @Override public void customizeJSONSerialization(ObjectMapper objectMapper) { }
}
