package com.chartanalysis.domain.trade;

import com.chartanalysis.domain.portfolio.Holding;
import com.chartanalysis.domain.portfolio.HoldingRepository;
import com.chartanalysis.domain.portfolio.Portfolio;
import com.chartanalysis.domain.portfolio.PortfolioRepository;
import com.chartanalysis.domain.stock.Stock;
import com.chartanalysis.domain.stock.StockRepository;
import com.chartanalysis.domain.trade.dto.TradeRequest;
import com.chartanalysis.domain.trade.dto.TradeResponse;
import com.chartanalysis.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;

    @Transactional
    public TradeResponse executeTrade(User user, TradeRequest request) {
        Stock stock = stockRepository.findBySymbol(request.getSymbol())
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + request.getSymbol()));

        Portfolio portfolio = portfolioRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));

        BigDecimal price = stock.getCurrentPrice();
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(request.getQuantity()));

        if (request.getType() == TradeType.BUY) {
            return executeBuy(user, stock, portfolio, request.getQuantity(), price, totalAmount);
        } else {
            return executeSell(user, stock, portfolio, request.getQuantity(), price, totalAmount);
        }
    }

    private TradeResponse executeBuy(User user, Stock stock, Portfolio portfolio,
                                      int quantity, BigDecimal price, BigDecimal totalAmount) {
        if (portfolio.getAvailableCash().compareTo(totalAmount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다. 현재 잔액: " + portfolio.getAvailableCash());
        }

        portfolio.setAvailableCash(portfolio.getAvailableCash().subtract(totalAmount));
        portfolioRepository.save(portfolio);

        Holding holding = holdingRepository.findByPortfolioAndStock(portfolio, stock)
                .orElse(Holding.builder().portfolio(portfolio).stock(stock).quantity(0).avgPrice(BigDecimal.ZERO).build());

        BigDecimal totalInvested = holding.getAvgPrice().multiply(BigDecimal.valueOf(holding.getQuantity()))
                .add(totalAmount);
        int newQuantity = holding.getQuantity() + quantity;
        holding.setAvgPrice(totalInvested.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP));
        holding.setQuantity(newQuantity);
        holdingRepository.save(holding);

        Trade trade = Trade.builder()
                .user(user).stock(stock).type(TradeType.BUY)
                .quantity(quantity).price(price).totalAmount(totalAmount)
                .build();
        tradeRepository.save(trade);

        return new TradeResponse(trade, portfolio.getAvailableCash());
    }

    private TradeResponse executeSell(User user, Stock stock, Portfolio portfolio,
                                       int quantity, BigDecimal price, BigDecimal totalAmount) {
        Holding holding = holdingRepository.findByPortfolioAndStock(portfolio, stock)
                .orElseThrow(() -> new IllegalStateException("보유하지 않은 종목입니다."));

        if (holding.getQuantity() < quantity) {
            throw new IllegalStateException("보유 수량이 부족합니다. 현재 보유: " + holding.getQuantity() + "주");
        }

        portfolio.setAvailableCash(portfolio.getAvailableCash().add(totalAmount));
        portfolioRepository.save(portfolio);

        if (holding.getQuantity() == quantity) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(holding.getQuantity() - quantity);
            holdingRepository.save(holding);
        }

        Trade trade = Trade.builder()
                .user(user).stock(stock).type(TradeType.SELL)
                .quantity(quantity).price(price).totalAmount(totalAmount)
                .build();
        tradeRepository.save(trade);

        return new TradeResponse(trade, portfolio.getAvailableCash());
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> getHistory(User user) {
        Portfolio portfolio = portfolioRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
        return tradeRepository.findByUserOrderByTradedAtDesc(user).stream()
                .map(trade -> new TradeResponse(trade, portfolio.getAvailableCash()))
                .collect(Collectors.toList());
    }
}
