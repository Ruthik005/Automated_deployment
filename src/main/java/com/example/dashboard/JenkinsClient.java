package com.example.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JenkinsClient {

    @Value("${jenkins.base-url}")
    private String baseUrl;

    @Value("${jenkins.user}")
    private String user;

    @Value("${jenkins.token}")
    private String token;

    private final RestTemplate rest = new RestTemplate();

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String creds = user + ":" + token;
        String basic = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + basic);
        return headers;
    }

    public String fetchConsoleText(String jobName) {
        String url = baseUrl + "/job/" + jobName + "/lastBuild/consoleText";
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, entity, String.class);
        return resp.getBody();
    }

    public JenkinsBuildInfo fetchLastBuildInfo(String jobName) {
        String url = baseUrl + "/job/" + jobName + "/lastBuild/api/json";
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<JenkinsBuildInfo> resp =
                rest.exchange(url, HttpMethod.GET, entity, JenkinsBuildInfo.class);
        return resp.getBody();
    }
}
