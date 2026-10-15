package com.hrstack.hr_stack.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
public class WebPushService {

    private final String vapidPublicKey;
    private final String vapidPrivateKey;

    public WebPushService(
            @Value("${vapid.public-key}") String vapidPublicKey,
            @Value("${vapid.private-key}") String vapidPrivateKey) {

        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;

        if (Security.getProvider(
                BouncyCastleProvider.PROVIDER_NAME
        ) == null) {
            Security.addProvider(
                    new BouncyCastleProvider()
            );
        }
    }

    public void sendPush(
            String endpoint,
            String p256dh,
            String auth,
            String payload
    ) throws Exception {

        Notification notification =
                Notification.builder()
                        .endpoint(endpoint)
                        .userPublicKey(p256dh)
                        .userAuth(auth)
                        .payload(payload)
                        .ttl(86400)
                        .urgency(Urgency.HIGH)
                        .build();

        PushService pushService =
                new PushService(
                        vapidPublicKey,
                        vapidPrivateKey,
                        "mailto:purvesh0227@gmail.com"
                );

        HttpResponse response = pushService.send(notification);

        int status = response.getStatusLine().getStatusCode();

        System.out.println(
                "WEB PUSH status " + status + " -> "
                        + endpoint.substring(0, Math.min(45, endpoint.length()))
        );

        if (status < 200 || status >= 300) {
            String body = "";
            if (response.getEntity() != null) {
                body = org.apache.http.util.EntityUtils
                        .toString(response.getEntity());
            }
            throw new RuntimeException(
                    "Web Push rejected, HTTP " + status + " " + body);
        }
    }
}