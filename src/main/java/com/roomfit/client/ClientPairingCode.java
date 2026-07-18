package com.roomfit.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 익명 clientId(X-RoomFit-Client-Id)를 다른 브라우저에 옮겨 붙이기 위한 영구
 * 페어링 코드. clientId당 코드 하나만 존재하며(만료/1회성 없음), 필요하면
 * {@link #rotateCode} 로 기존 코드를 무효화하고 새 코드를 발급한다 — clientId
 * 자체는 이미 handoff URL에 평문으로 노출되는 값이라(로그인 비밀값 취급하지
 * 않음, ClientScope 참고) 코드도 같은 수준의 민감도로 다룬다.
 */
@Entity
@Table(name = "client_pairing_code")
public class ClientPairingCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "code", nullable = false, unique = true, length = 8)
    private String code;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ClientPairingCode() {
        // JPA용
    }

    public ClientPairingCode(String clientId, String code) {
        this.clientId = clientId;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void rotateCode(String newCode) {
        this.code = newCode;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
