// Copyright (c) 2023, Cisco and/or its affiliates.
// All rights reserved.
// See license in distribution for details.

package com.example.tiediesampleapp.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.cisco.tiedie.auth.ApiKeyAuthenticator;
import com.cisco.tiedie.auth.Authenticator;
import com.cisco.tiedie.auth.CertificateAuthenticator;
import com.cisco.tiedie.clients.ControlClient;
import com.cisco.tiedie.clients.OnboardingClient;
import com.cisco.tiedie.dto.HttpResponse;
import com.cisco.tiedie.dto.scim.AppCertificateInfo;
import com.cisco.tiedie.dto.scim.EndpointApp;
import com.cisco.tiedie.dto.scim.EndpointAppType;

@Component
@Configuration
public class ControlAppConfig extends ClientConfig {

    @Value("${control-app.id}")
    private String controlAppId;

    @Value("${control-app.auth-type}")
    private String controlAppAuthType;

    private static final String CONTROL_CERT_PATH = "/control-app.p12";
    private static final String CONTROL_BASE_URL = "https://localhost:8081/control";

    private EndpointApp createEndpointApp(OnboardingClient onboardingClient) throws Exception {
        var controlAppBuilder = EndpointApp.builder()
                .applicationName(controlAppId)
                .applicationType(EndpointAppType.DEVICE_CONTROL);

        if (controlAppAuthType.equals("cert")) {
            InputStream caStream = ControlAppConfig.class.getResourceAsStream(CA_PEM_PATH);
            InputStream clientKeystoreStream = ControlAppConfig.class.getResourceAsStream(CONTROL_CERT_PATH);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(clientKeystoreStream, "".toCharArray());

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(caStream);

            var rootCN = getCnFromCert(caCert);
            var cn = getCnFromKeyStore(keyStore);

            controlAppBuilder = controlAppBuilder
                    .certificateInfo(AppCertificateInfo.builder()
                            .rootCN(rootCN)
                            .subjectName(cn)
                            .build());
        }

        HttpResponse<EndpointApp> createEndpointAppResponse = onboardingClient
                .createEndpointApp(controlAppBuilder.build());

        return createEndpointAppResponse.getBody();
    }

    @Autowired
    @Bean
    @Qualifier("controlApp")
    public EndpointApp getControlAppEndpointApp(OnboardingClient onboardingClient) throws Exception {
        HttpResponse<List<EndpointApp>> httpResponse = onboardingClient.getEndpointApps();

        List<EndpointApp> endpointApps = httpResponse.getBody();

        if (endpointApps == null) {
            endpointApps = List.of();
        }

        return endpointApps.stream()
                .filter(app -> app.getApplicationType() == EndpointAppType.DEVICE_CONTROL
                        && app.getApplicationName().equals(controlAppId))
                .findFirst()
                .orElse(createEndpointApp(onboardingClient));
    }

    public Authenticator getAuthenticator(EndpointApp endpointApp) throws Exception {
        try (InputStream caStream = ControlAppConfig.class.getResourceAsStream(CA_PEM_PATH);
                InputStream clientKeystoreStream = ControlAppConfig.class.getResourceAsStream(CONTROL_CERT_PATH)) {
            if (endpointApp.getCertificateInfo() != null) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(clientKeystoreStream, "".toCharArray());

                return CertificateAuthenticator.create(caStream, keyStore, "");
            }

            return ApiKeyAuthenticator.create(caStream, controlAppId,
                    endpointApp.getClientToken());
        }
    }

    @Autowired
    @Bean
    public ControlClient getControlClient(OnboardingClient onboardingClient, @Qualifier("controlApp") EndpointApp endpointApp) throws Exception {
        Authenticator authenticator = getAuthenticator(endpointApp);

        return new ControlClient(CONTROL_BASE_URL, authenticator);
    }
}
