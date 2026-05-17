package com.chartanalysis.domain.portfolio;

import com.chartanalysis.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByPortfolio(Portfolio portfolio);
    Optional<Holding> findByPortfolioAndStock(Portfolio portfolio, Stock stock);
}
