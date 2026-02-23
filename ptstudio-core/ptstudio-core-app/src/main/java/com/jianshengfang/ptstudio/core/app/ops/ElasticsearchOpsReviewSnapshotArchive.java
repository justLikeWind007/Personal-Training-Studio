package com.jianshengfang.ptstudio.core.app.ops;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("es")
public class ElasticsearchOpsReviewSnapshotArchive implements OpsReviewSnapshotArchive {

    private static final String INDEX = "ptstudio_ops_review_snapshot";
    private static final String INDEX_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "dynamic": "strict",
                "properties": {
                  "tenantId": { "type": "keyword" },
                  "storeId": { "type": "keyword" },
                  "dateFrom": { "type": "date" },
                  "dateTo": { "type": "date" },
                  "totalTasks": { "type": "integer" },
                  "doneTasks": { "type": "integer" },
                  "overdueTasks": { "type": "integer" },
                  "touchCount": { "type": "integer" },
                  "convertedCount": { "type": "integer" },
                  "completionRate": { "type": "double" },
                  "overdueRate": { "type": "double" },
                  "conversionRate": { "type": "double" },
                  "avgHandleHours": { "type": "double" },
                  "generatedAt": { "type": "date" }
                }
              }
            }
            """;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean indexChecked = new AtomicBoolean(false);

    private final String esBaseUrl;

    public ElasticsearchOpsReviewSnapshotArchive() {
        String scheme = System.getenv().getOrDefault("ES_SCHEME", "http");
        String host = System.getenv().getOrDefault("ES_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("ES_PORT", "9200");
        this.esBaseUrl = scheme + "://" + host + ":" + port;
    }

    @Override
    public void saveLatest(String tenantId,
                           String storeId,
                           OpsReviewDashboardService.ReviewSnapshot snapshot) {
        try {
            ensureIndex();
            String docId = docId(tenantId, storeId);
            Map<String, Object> body = new HashMap<>();
            body.put("tenantId", tenantId);
            body.put("storeId", storeId);
            body.put("dateFrom", snapshot.dateFrom().toString());
            body.put("dateTo", snapshot.dateTo().toString());
            body.put("totalTasks", snapshot.totalTasks());
            body.put("doneTasks", snapshot.doneTasks());
            body.put("overdueTasks", snapshot.overdueTasks());
            body.put("touchCount", snapshot.touchCount());
            body.put("convertedCount", snapshot.convertedCount());
            body.put("completionRate", snapshot.completionRate());
            body.put("overdueRate", snapshot.overdueRate());
            body.put("conversionRate", snapshot.conversionRate());
            body.put("avgHandleHours", snapshot.avgHandleHours());
            body.put("generatedAt", OffsetDateTime.now().toString());

            HttpRequest request = HttpRequest.newBuilder(URI.create(esBaseUrl + "/" + INDEX + "/_doc/" + docId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(response.statusCode())) {
                return;
            }
        } catch (Exception ignored) {
            // ES 不可用时降级，不影响主链路返回
        }
    }

    @Override
    public Optional<OpsReviewDashboardService.ArchivedReviewSnapshot> latest(String tenantId, String storeId) {
        try {
            ensureIndex();
            String docId = docId(tenantId, storeId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(esBaseUrl + "/" + INDEX + "/_doc/" + docId))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object found = payload.get("found");
            if (!(found instanceof Boolean) || !(Boolean) found) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) payload.get("_source");
            if (source == null) {
                return Optional.empty();
            }
            return Optional.of(new OpsReviewDashboardService.ArchivedReviewSnapshot(
                    str(source.get("tenantId")),
                    str(source.get("storeId")),
                    LocalDate.parse(str(source.get("dateFrom"))),
                    LocalDate.parse(str(source.get("dateTo"))),
                    intValue(source.get("totalTasks")),
                    intValue(source.get("doneTasks")),
                    intValue(source.get("overdueTasks")),
                    intValue(source.get("touchCount")),
                    intValue(source.get("convertedCount")),
                    decimalValue(source.get("completionRate")),
                    decimalValue(source.get("overdueRate")),
                    decimalValue(source.get("conversionRate")),
                    decimalValue(source.get("avgHandleHours")),
                    OffsetDateTime.parse(str(source.get("generatedAt")))
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void ensureIndex() {
        if (indexChecked.compareAndSet(false, true)) {
            try {
                HttpRequest check = HttpRequest.newBuilder(URI.create(esBaseUrl + "/" + INDEX))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> checkResponse = httpClient.send(check, HttpResponse.BodyHandlers.ofString());
                if (checkResponse.statusCode() == 200) {
                    return;
                }
                if (checkResponse.statusCode() != 404) {
                    return;
                }

                HttpRequest create = HttpRequest.newBuilder(URI.create(esBaseUrl + "/" + INDEX))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(INDEX_MAPPING))
                        .build();
                HttpResponse<String> createResponse = httpClient.send(create, HttpResponse.BodyHandlers.ofString());
                if (!(createResponse.statusCode() == 200 || createResponse.statusCode() == 201)) {
                    return;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private String docId(String tenantId, String storeId) {
        return URLEncoder.encode(tenantId + "_" + storeId, StandardCharsets.UTF_8);
    }

    private String str(Object source) {
        return source == null ? "" : String.valueOf(source);
    }

    private Integer intValue(Object source) {
        return source == null ? 0 : Integer.parseInt(String.valueOf(source));
    }

    private BigDecimal decimalValue(Object source) {
        return source == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(source));
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
