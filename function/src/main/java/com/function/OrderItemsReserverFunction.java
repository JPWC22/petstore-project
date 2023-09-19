package com.function;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

public class OrderItemsReserverFunction {
    @FunctionName("OrderItemsReserver")
    public void run(
            @ServiceBusQueueTrigger(name = "orderItemsReserver", queueName = "petstore_order", connection = "ServiceBusConnectionString") String orderRequestJson,
            final ExecutionContext context) {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> requestBody;
        try {
            requestBody = objectMapper.readValue(orderRequestJson, Map.class);
        } catch (IOException e) {
            context.getLogger().warning("Failed to parse order request JSON");
            return;
        }

        String connectionString = System.getenv("BlobStorageConnectionString");
        String containerName = "order-requests";
        String sessionId = String.valueOf(requestBody.get("id"));
        String blobName = sessionId + ".json";

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString)
                .buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        byte[] data = orderRequestJson.getBytes();

        int retries = 3;
        boolean uploadSuccess = false;

        while (retries > 0) {
            try {
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                    blobClient.upload(inputStream, data.length, true);
                }
                context.getLogger().info("Order request uploaded to Blob Storage.");
                uploadSuccess = true;
                break;
            } catch (IOException | BlobStorageException a) {
                context.getLogger().warning("Error uploading order request to blob storage");
                retries--;
            }
        }

        if (!uploadSuccess) {
            triggerLogicApp(orderRequestJson, context);
        }
    }

    private void triggerLogicApp(String orderRequestJson, ExecutionContext context) {

        String logicAppUrl = System.getenv("LogicAppUrl");

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(logicAppUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(orderRequestJson))
                .build();

        HttpResponse response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response != null && response.statusCode() == 202) {
                context.getLogger().info("Successfully triggered the Logic App");
            } else {
                context.getLogger().warning("Failed to trigger the Logic App");
            }
        } catch (Exception e) {
            context.getLogger().warning("Error triggering the Logic App: " + e.getMessage());
        }
    }

}
