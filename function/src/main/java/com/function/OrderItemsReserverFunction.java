package com.function;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
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
        try {
            String connectionString = System.getenv("BlobStorageConnectionString");
            String containerName = "order-requests";
            String sessionId = String.valueOf(requestBody.get("id"));
            String blobName = sessionId + ".json";

            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString)
                    .buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            byte[] data = orderRequestJson.getBytes();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                blobClient.upload(inputStream, data.length, true);
            }
            context.getLogger().info("Order request uploaded to Blob Storage.");
        } catch (IOException | BlobStorageException e) {
            context.getLogger().warning("Error uploading order request to blob storage");
        }
    }
}