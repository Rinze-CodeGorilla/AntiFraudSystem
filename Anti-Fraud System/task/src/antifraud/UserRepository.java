package antifraud;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends CrudRepository<User, Integer> {
    Optional<User> findUserByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByOrderById();
    boolean existsBy();
}
