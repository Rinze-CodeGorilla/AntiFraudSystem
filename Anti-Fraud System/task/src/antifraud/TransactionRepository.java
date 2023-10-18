package antifraud;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface TransactionRepository extends CrudRepository<Transaction, Integer> {
    List<RegionOnly> findDistinctRegionByRegionIsNotAndDateBetweenAndNumber(Region region, LocalDateTime dateFrom, LocalDateTime dateTo, String number);
    List<IpOnly> findDistinctIpByIpIsNotAndDateBetweenAndNumber(String ip, LocalDateTime dateFrom, LocalDateTime dateTo, String number);
    List<Transaction> findByOrderById();
    List<Transaction> findByNumberOrderById(String number);
}

record RegionOnly(Region region) {}
record IpOnly(String ip) {}