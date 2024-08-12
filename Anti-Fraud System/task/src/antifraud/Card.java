package antifraud;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String number;

    Card(String number) {
        this.number = number;
    }

    public Card() {

    }

    static boolean IsValidLuhn(String number) {
        int[] digits = number.chars().map(c -> c - '0').toArray();
        int check_digit = 0;
        for (int i = digits.length - 2; i >= 0; --i) {
            check_digit += switch (i % 2) {
                case 0 -> digits[i] > 4 ? digits[i] * 2 - 9 : digits[i] * 2;
                case 1 -> digits[i];
                default -> throw new IllegalStateException("Unexpected value: " + i % 2);
            };
        }
        return (check_digit + digits[digits.length - 1]) % 10 == 0;
    }
}
