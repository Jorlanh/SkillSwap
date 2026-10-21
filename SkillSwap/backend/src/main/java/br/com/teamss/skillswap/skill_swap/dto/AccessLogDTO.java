package br.com.teamss.skillswap.skill_swap.dto;

import java.sql.Timestamp;
import java.time.Instant;

public class AccessLogDTO {
    private Instant  accessTime;
    private String ipAddress;
    private String location;
    private String city;
    private String subdivision;
    private String country;

    public AccessLogDTO(Instant  accessTime, String ipAddress, String location, String city, String subdivision, String country) {
        this.accessTime = accessTime;
        this.ipAddress = ipAddress;
        this.location = location;
        this.city = city;
        this.subdivision = subdivision;
        this.country = country;
    }

    public Instant  getAccessTime() { return accessTime; }
    public void setAccessTime(Instant  accessTime) { this.accessTime = accessTime; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSubdivision() { return subdivision; }
    public void setSubdivision(String subdivision) { this.subdivision = subdivision; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}