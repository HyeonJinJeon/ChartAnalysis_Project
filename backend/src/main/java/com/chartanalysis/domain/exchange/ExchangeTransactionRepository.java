package com.chartanalysis.domain.exchange;

import com.chartanalysis.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeTransactionRepository extends JpaRepository<ExchangeTransaction, Long> {
    List<ExchangeTransaction> findByUserOrderByCreatedAtDesc(User user);
}
