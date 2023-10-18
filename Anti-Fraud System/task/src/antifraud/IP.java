package antifraud;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Arrays;

@Entity
class IP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String ip;

    IP(String ip) {
        this.ip = ip;
    }

    public IP() {

    }

    public static boolean verify(String ip) {
        var parts = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
        if (parts.length != 4) return false;
        if (Arrays.stream(parts).anyMatch(b -> b < 0 || b > 255)) return false;
        return true;
    }
}
