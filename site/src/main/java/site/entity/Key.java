package site.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "keys")
public class Key {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "inbound_id", nullable = false)
    private Integer inboundId;

    @Column(name = "xui_client_id", nullable = false)
    private String xuiClientId;

    @Column(name = "xui_email", nullable = false)
    private String xuiEmail;

    @Column(name = "remark")
    private String remark;

    @Column(name = "expiry_ts")
    private Long expiryTs = 0L;

    @Column(name = "traffic_total")
    private Long trafficTotal = 0L;

    @Column(name = "traffic_up")
    private Long trafficUp = 0L;

    @Column(name = "traffic_down")
    private Long trafficDown = 0L;

    @Column(name = "created_at")
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getInboundId() { return inboundId; }
    public void setInboundId(Integer inboundId) { this.inboundId = inboundId; }

    public String getXuiClientId() { return xuiClientId; }
    public void setXuiClientId(String xuiClientId) { this.xuiClientId = xuiClientId; }

    public String getXuiEmail() { return xuiEmail; }
    public void setXuiEmail(String xuiEmail) { this.xuiEmail = xuiEmail; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getExpiryTs() { return expiryTs; }
    public void setExpiryTs(Long expiryTs) { this.expiryTs = expiryTs; }

    public Long getTrafficTotal() { return trafficTotal; }
    public void setTrafficTotal(Long trafficTotal) { this.trafficTotal = trafficTotal; }

    public Long getTrafficUp() { return trafficUp; }
    public void setTrafficUp(Long trafficUp) { this.trafficUp = trafficUp; }

    public Long getTrafficDown() { return trafficDown; }
    public void setTrafficDown(Long trafficDown) { this.trafficDown = trafficDown; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public double trafficUsedGb() {
        return (trafficUp + trafficDown) / (1024.0 * 1024.0 * 1024.0);
    }

    public double trafficTotalGb() {
        return trafficTotal / (1024.0 * 1024.0 * 1024.0);
    }

    public boolean isExpired() {
        return expiryTs > 0 && System.currentTimeMillis() > expiryTs;
    }
}
