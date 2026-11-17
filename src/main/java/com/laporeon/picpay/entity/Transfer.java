package com.laporeon.picpay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tbl_transfers")
@EntityListeners(AuditingEntityListener.class)
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_wallet_id", nullable = false)
    private Wallet payee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_wallet_id", nullable = false)
    private Wallet payer;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Transfer() {}

    public Transfer(Wallet payee, Wallet payer, BigDecimal amount) {
        this.payee = payee;
        this.payer = payer;
        this.amount = amount;
    }

    public Wallet getPayee() {
        return payee;
    }

    public Wallet getPayer() {
        return payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
