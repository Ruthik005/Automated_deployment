package com.example.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JenkinsBuildInfo {
    private Integer number;
    private Boolean building;
    private String result;
    private Long timestamp;
    private String url;

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public Boolean getBuilding() { return building; }
    public void setBuilding(Boolean building) { this.building = building; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
