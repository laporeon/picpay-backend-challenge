package com.laporeon.picpay.repository;

import com.laporeon.picpay.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByDocumentOrEmail(String document, String email);
}
