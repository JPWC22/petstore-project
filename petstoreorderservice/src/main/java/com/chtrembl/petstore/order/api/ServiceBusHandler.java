package com.chtrembl.petstore.order.api;

import org.springframework.stereotype.Service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

@Service
public class ServiceBusHandler {
    // private final String connectionString = System.getenv("ServiceBusConnectionString");
    private final String connectionString = "Endpoint=sb://petstore901.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=LpQc07QVqy69qTMPGLjOwRfgFepzdC6Ao+ASbOTtlE4=";
    private final String queueName = "petstore_order";
        
    public void sendMessage(String message) {
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueName)
            .buildClient();
            
        senderClient.sendMessage(new ServiceBusMessage(message));
        senderClient.close();
    }
}