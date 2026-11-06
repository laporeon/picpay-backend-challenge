package com.laporeon.picpay.entity;

import com.laporeon.picpay.enums.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tbl_wallets")
@EntityListeners(AuditingEntityListener.class)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true)
    private  String document;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column(name = "wallet_type")
    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    @Column(name = "balance")
    private BigDecimal balance;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Wallet(String fullName, String document, String email, String password, WalletType walletType) {
        this.fullName = fullName;
        this.document = document;
        this.email = email;
        this.password = password;
        this.walletType = walletType;
        this.balance = BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public void setWalletType(WalletType walletType) {
        this.walletType = walletType;
    }

    public BigDecimal getBalance() {
        return balance;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}
