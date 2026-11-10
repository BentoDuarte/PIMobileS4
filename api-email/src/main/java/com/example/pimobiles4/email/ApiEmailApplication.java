package com.example.pimobiles4.email;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ConfiguracaoImportacao.class)
public class ApiEmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiEmailApplication.class, args);
    }

    @Bean(destroyMethod = "delete")
    FirebaseApp firebase(ConfiguracaoImportacao config) throws IOException {
        return FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(config.projetoFirebase())
                .build());
    }

    @Bean(destroyMethod = "")
    Firestore firestore(FirebaseApp firebase) {
        return FirestoreClient.getFirestore(firebase);
    }
}
