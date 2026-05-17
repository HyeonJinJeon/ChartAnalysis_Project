package com.chartanalysis.domain.trade;

import com.chartanalysis.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserOrderByTradedAtDesc(User user);
}
