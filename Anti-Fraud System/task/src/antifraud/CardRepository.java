package antifraud;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

interface CardRepository extends CrudRepository<Card, Integer> {
    Optional<Card> findByNumber(String number);
    boolean existsByNumber(String number);
    List<Card> findByOrderById();
}
