package antifraud;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
public class TransactionChecker {
    private final IpRepository ipRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    TransactionChecker(IpRepository ipRepository, CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.ipRepository = ipRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    Map<Function<TransactionController.TransactionRequest, TransactionResult>, String> getChecks() {
        return Map.of(
                this::checkAmount, "amount",
                this::checkIp, "ip",
                this::checkCardNumber, "card-number",
                this::checkRegionCorrelation, "region-correlation",
                this::checkIpCorrelation, "ip-correlation"
        );
    }

    TransactionResult checkIp(TransactionController.TransactionRequest t) {
        return ipRepository.existsByIp(t.ip()) ? TransactionResult.PROHIBITED : TransactionResult.ALLOWED;
    }

    TransactionResult checkCardNumber(TransactionController.TransactionRequest t) {
        return cardRepository.existsByNumber(t.number()) ? TransactionResult.PROHIBITED : TransactionResult.ALLOWED;
    }

    TransactionResult checkRegionCorrelation(TransactionController.TransactionRequest t) {
        var regions = transactionRepository.findDistinctRegionByRegionIsNotAndDateBetweenAndNumber(
                t.region(), t.date().minusHours(1), t.date(), t.number());
        if (regions.size() > 2) {
            return TransactionResult.PROHIBITED;
        } else if (regions.size() == 2) {
            return TransactionResult.MANUAL_PROCESSING;
        }
        return TransactionResult.ALLOWED;
    }

    TransactionResult checkIpCorrelation(TransactionController.TransactionRequest t) {
        var ips = transactionRepository.findDistinctIpByIpIsNotAndDateBetweenAndNumber(
                t.ip(), t.date().minusHours(1), t.date(), t.number());
        if (ips.size() > 2) {
            return TransactionResult.PROHIBITED;
        } else if (ips.size() == 2) {
            return TransactionResult.MANUAL_PROCESSING;
        }
        return TransactionResult.ALLOWED;
    }

    TransactionResult checkAmount(TransactionController.TransactionRequest t) {
        var previousTransactions = transactionRepository.findByNumberOrderById(t.number());
        long allowedAmount = previousTransactions.stream().reduce(200L, this::processAllowedAmount, (a, b) -> {
            throw new RuntimeException("Transaction processing can only occur in order");
        });
        long manualAmount = previousTransactions.stream().reduce(1500L, this::processManualAmount, (a, b) -> {
            throw new RuntimeException("Transaction processing can only occur in order");
        });
        if (t.amount() > manualAmount) {
            return TransactionResult.PROHIBITED;
        }
        if (t.amount() > allowedAmount) {
            return TransactionResult.MANUAL_PROCESSING;
        }
        return TransactionResult.ALLOWED;
    }

    private long processAllowedAmount(long currentAmount, Transaction t) {
        if (t.feedback == TransactionResult.ALLOWED) return adjustUp(currentAmount, t.amount);
        if (t.feedback != null && t.result == TransactionResult.ALLOWED) return adjustDown(currentAmount, t.amount);
        return currentAmount;
    }

    private long processManualAmount(long currentAmount, Transaction t) {
        if (t.feedback == TransactionResult.PROHIBITED) return adjustDown(currentAmount, t.amount);
        if (t.feedback != null && t.result == TransactionResult.PROHIBITED) return adjustUp(currentAmount, t.amount);
        return currentAmount;
    }

    private long adjustUp(long oldAmount, long transactionAmount) {
        return (long) Math.ceil(.8 * oldAmount + .2 * transactionAmount);
    }

    private long adjustDown(long oldAmount, long transactionAmount) {
        return (long) Math.ceil(.8 * oldAmount - .2 * transactionAmount);
    }

}
