package com.example.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jenkins")
public class JenkinsController {

    private final JenkinsClient client;

    @Value("${jenkins.job}")
    private String defaultJob;

    public JenkinsController(JenkinsClient client) {
        this.client = client;
    }

    @GetMapping("/console")
    public ResponseEntity<String> console(@RequestParam(required = false) String job) {
        String jobName = defaultJob;
        if (job != null) {
            if (!job.isEmpty()) {
                jobName = job;
            }
        }
        try {
            String text = client.fetchConsoleText(jobName);
            return ResponseEntity.ok(text);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching Jenkins logs: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<JenkinsBuildInfo> status(@RequestParam(required = false) String job) {
        String jobName = defaultJob;
        if (job != null) {
            if (!job.isEmpty()) {
                jobName = job;
            }
        }
        try {
            JenkinsBuildInfo info = client.fetchLastBuildInfo(jobName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
