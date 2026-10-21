package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_access_logs")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "access_time")
    private Instant  accessTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "location")
    private String location;

    @Column(name = "city")
    private String city;

    @Column(name = "subdivision")
    private String subdivision;

    @Column(name = "country")
    private String country;

    // Construtores
    public AccessLog() {}

    public AccessLog(UUID userId, String ipAddress, String location, String city, String subdivision, String country) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.location = location;
        this.city = city;
        this.subdivision = subdivision;
        this.country = country;
    }

    // Getters e Setters
    public Long getAccessId() { return accessId; }
    public void setAccessId(Long accessId) { this.accessId = accessId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
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