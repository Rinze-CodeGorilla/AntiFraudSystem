package antifraud;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    long amount;
    String ip;

    String number;
    @Enumerated(EnumType.STRING)
    Region region;
    LocalDateTime date;
    @Enumerated(EnumType.STRING)
    TransactionResult result;
    @Enumerated(EnumType.STRING)
    TransactionResult feedback;

    public Transaction() {
    }

    public Transaction(long amount, String ip, String number, Region region, LocalDateTime date, TransactionResult result) {
        this.amount = amount;
        this.ip = ip;
        this.number = number;
        this.region = region;
        this.date = date;
        this.result = result;
    }
}
