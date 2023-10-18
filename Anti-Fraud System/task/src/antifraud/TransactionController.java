package antifraud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/antifraud")
public class TransactionController {

    private final IpRepository ipRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    TransactionController(IpRepository ipRepository, CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.ipRepository = ipRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/transaction")
    TransactionResultResponse handle(@Valid @RequestBody TransactionRequest transaction) throws BadRequestException {
        if (!Card.IsValidLuhn(transaction.number)) throw new BadRequestException();
        if (!IP.verify(transaction.ip)) throw new BadRequestException();
        List<String> manualInfos = new ArrayList<>();
        List<String> prohibitedInfos = new ArrayList<>();
        long manualAmount = 1500;
        long allowedAmount = 200;
        var previousTransactions = transactionRepository.findByNumberOrderById(transaction.number);
        LongBinaryOperator adjustUp = (old, amount) -> (long)Math.ceil(.8 * old + .2 * amount);
        LongBinaryOperator adjustDown = (old, amount) -> (long)Math.ceil(.8 * old - .2 * amount);
        for (var t :
                previousTransactions) {
            if (t.feedback == TransactionResult.ALLOWED) {
                allowedAmount = adjustUp.applyAsLong(allowedAmount, t.amount);
            }
            if (t.feedback == TransactionResult.PROHIBITED) {
                manualAmount = adjustDown.applyAsLong(manualAmount, t.amount);
            }
            if (t.feedback != null) {
                if (t.result == TransactionResult.ALLOWED) {
                    allowedAmount = adjustDown.applyAsLong(allowedAmount, t.amount);
                }
                if (t.result == TransactionResult.PROHIBITED) {
                    manualAmount = adjustUp.applyAsLong(manualAmount, t.amount);
                }
            }
        }
        TransactionResult status;
        if (transaction.amount <= allowedAmount) {
            //do nothing
        } else if (transaction.amount <= manualAmount) {
            manualInfos.add("amount");
        } else {
            prohibitedInfos.add("amount");
        }
        if (ipRepository.existsByIp(transaction.ip)) {
            prohibitedInfos.add("ip");
        }
        if (cardRepository.existsByNumber(transaction.number)) {
            prohibitedInfos.add("card-number");
        }
        var regions = transactionRepository.findDistinctRegionByRegionIsNotAndDateBetweenAndNumber(
                transaction.region, transaction.date.minusHours(1), transaction.date, transaction.number);
        if (regions.size() > 2) {
            prohibitedInfos.add("region-correlation");
        } else if (regions.size() == 2) {
            manualInfos.add("region-correlation");
        }
        var ips = transactionRepository.findDistinctIpByIpIsNotAndDateBetweenAndNumber(
                transaction.ip, transaction.date.minusHours(1), transaction.date, transaction.number);
        if (ips.size() > 2) {
            prohibitedInfos.add("ip-correlation");
        } else if (ips.size() == 2) {
            manualInfos.add("ip-correlation");
        }
        List<String> infos;
        if (!prohibitedInfos.isEmpty()) {
            infos = prohibitedInfos;
            status = TransactionResult.PROHIBITED;
        } else if (!manualInfos.isEmpty()) {
            infos = manualInfos;
            status = TransactionResult.MANUAL_PROCESSING;
        } else {
            infos = List.of();
            status = TransactionResult.ALLOWED;
        }
        String info = infos.stream().sorted().collect(Collectors.joining(", "));
        if (info.isEmpty()) info = "none";
        transactionRepository.save(new Transaction(
                transaction.amount,
                transaction.ip,
                transaction.number,
                transaction.region,
                transaction.date,
                status
        ));
        return new TransactionResultResponse(status, info);
    }

    @PostMapping("/suspicious-ip")
    IPResponse addIP(@Valid @RequestBody IPRequest request) throws ConflictException, BadRequestException {
        if (ipRepository.existsByIp(request.ip)) throw new ConflictException();
        if (!IP.verify(request.ip)) throw new BadRequestException();
        var ip = new IP(request.ip);
        return new IPResponse(ipRepository.save(ip));
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    IPDeletedResponse deleteIP(@PathVariable String ip) throws NotFoundException, BadRequestException {
        if (!IP.verify(ip)) throw new BadRequestException();
        ipRepository.delete(ipRepository.findByIp(ip).orElseThrow(NotFoundException::new));
        return new IPDeletedResponse("IP %s successfully removed!".formatted(ip));
    }

    @GetMapping("/suspicious-ip")
    Stream<IPResponse> listIP() {
        return ipRepository.findByOrderById().stream().map(IPResponse::new);
    }

    @PostMapping("/stolencard")
    CardResponse addCard(@Valid @RequestBody CardRequest request) throws ConflictException, BadRequestException {
        if (cardRepository.existsByNumber(request.number)) throw new ConflictException();
        if (!Card.IsValidLuhn(request.number)) throw new BadRequestException();
        var card = new Card(request.number);
        return new CardResponse(cardRepository.save(card));
    }

    @DeleteMapping("/stolencard/{number}")
    CardDeletedResponse deleteCard(@PathVariable String number) throws NotFoundException, BadRequestException {
        if (!Card.IsValidLuhn(number)) throw new BadRequestException();
        cardRepository.delete(cardRepository.findByNumber(number).orElseThrow(NotFoundException::new));
        return new CardDeletedResponse("Card %s successfully removed!".formatted(number));
    }

    @GetMapping("/stolencard")
    Stream<CardResponse> listCard() {
        return cardRepository.findByOrderById().stream().map(CardResponse::new);
    }

    @GetMapping("/history")
    Stream<TransactionResponse> history() {
        return transactionRepository.findByOrderById().stream().map(TransactionResponse::new);
    }

    @GetMapping("/history/{number}")
    Stream<TransactionResponse> historyForNumber(@PathVariable String number) throws NotFoundException, BadRequestException {
        if (!Card.IsValidLuhn(number)) throw new BadRequestException();
        var transactions = transactionRepository.findByNumberOrderById(number);
        if (transactions.isEmpty()) throw new NotFoundException();
        return transactions.stream().map(TransactionResponse::new);
    }

    @PutMapping("/transaction")
    TransactionResponse feedback(@Valid @RequestBody FeedbackRequest feedback) throws NotFoundException, UnprocessableEntityException, ConflictException {
        var transaction = transactionRepository.findById(feedback.transactionId).orElseThrow(NotFoundException::new);
        if (feedback.feedback == transaction.result) throw new UnprocessableEntityException();
        if (transaction.feedback != null) throw new ConflictException();
        transaction.feedback = feedback.feedback;
        return new TransactionResponse(transactionRepository.save(transaction));
    }

    record TransactionRequest(@Min(1) long amount, @NotNull String ip, @NotNull String number, @NotNull Region region,
                              @NotNull LocalDateTime date) {
    }

    record TransactionResultResponse(TransactionResult result, String info) {
    }

    record TransactionResponse(int transactionId, long amount, String ip, String number, Region region,
                               LocalDateTime date, TransactionResult result, String feedback) {
        TransactionResponse(Transaction t) {
            this(t.id, t.amount, t.ip, t.number, t.region, t.date, t.result, t.feedback == null ? "" : t.feedback.name());
        }
    }

    record IPRequest(@NotNull String ip) {
    }

    record IPResponse(int id, String ip) {
        IPResponse(IP ip) {
            this(ip.id, ip.ip);
        }
    }

    record IPDeletedResponse(String status) {
    }

    record CardRequest(@NotNull String number) {
    }

    record CardResponse(int id, String number) {
        CardResponse(Card card) {
            this(card.id, card.number);
        }
    }

    record CardDeletedResponse(String status) {
    }

    record FeedbackRequest(@NotNull int transactionId, @NotNull TransactionResult feedback) {
    }
}
