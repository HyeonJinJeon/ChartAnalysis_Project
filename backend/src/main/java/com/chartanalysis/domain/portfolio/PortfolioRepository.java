package com.chartanalysis.domain.portfolio;

import com.chartanalysis.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUser(User user);
    Optional<Portfolio> findByUserId(Long userId);
}
