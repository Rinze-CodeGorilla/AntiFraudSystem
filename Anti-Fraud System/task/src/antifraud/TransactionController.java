package antifraud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/antifraud")
public class TransactionController {

    private final IpRepository ipRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionChecker transactionChecker;

    TransactionController(IpRepository ipRepository, CardRepository cardRepository, TransactionRepository transactionRepository, TransactionChecker transactionChecker) {
        this.ipRepository = ipRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.transactionChecker = transactionChecker;
    }

    @PostMapping("/transaction")
    TransactionResultResponse handle(@Valid @RequestBody TransactionRequest transaction) throws BadRequestException {
        // switched to custom annotation in TransactionRequest
        //        if (!Card.IsValidLuhn(transaction.number)) throw new BadRequestException();
        if (!IP.verify(transaction.ip)) throw new BadRequestException();
        List<String> manualInfos = new ArrayList<>();
        List<String> prohibitedInfos = new ArrayList<>();

        transactionChecker.getChecks().entrySet().forEach((check -> {
            var method = check.getKey();
            var description = check.getValue();
            switch (method.apply(transaction)) {
                case MANUAL_PROCESSING -> manualInfos.add(description);
                case PROHIBITED -> prohibitedInfos.add(description);
            }
        }));

        List<String> infos;
        TransactionResult status;
        if (!prohibitedInfos.isEmpty()) {
            infos = prohibitedInfos;
            status = TransactionResult.PROHIBITED;
        } else if (!manualInfos.isEmpty()) {
            infos = manualInfos;
            status = TransactionResult.MANUAL_PROCESSING;
        } else {
            infos = List.of("none");
            status = TransactionResult.ALLOWED;
        }
        String info = infos.stream().sorted().collect(Collectors.joining(", "));
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

    record TransactionRequest(@Min(1) long amount, @NotNull String ip, @NotNull @Luhn String number, @NotNull Region region,
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
