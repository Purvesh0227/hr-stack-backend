package com.hrstack.hr_stack.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
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
                new Notification(
                        endpoint,
                        p256dh,
                        auth,
                        payload
                );

        PushService pushService =
                new PushService(
                        vapidPublicKey,
                        vapidPrivateKey,
                        "mailto:purvesh0227@gmail.com"
                );

        pushService.send(notification);
    }
}