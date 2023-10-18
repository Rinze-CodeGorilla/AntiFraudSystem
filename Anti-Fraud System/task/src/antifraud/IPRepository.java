package antifraud;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

interface IpRepository extends CrudRepository<IP, Integer> {
    Optional<IP> findByIp(String ip);
    boolean existsByIp(String ip);
    List<IP> findByOrderById();
}
