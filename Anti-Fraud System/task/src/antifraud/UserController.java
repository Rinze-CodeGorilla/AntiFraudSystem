package antifraud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

@RestController
@RequestMapping("/api/auth")
@Validated
public class UserController {
    private final UserRepository repository;

    UserController(UserRepository repository) {
        this.repository = repository;
    }

    @Autowired
    PasswordEncoder pw;

    @PostMapping("/user")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createUser(@Valid @RequestBody CreateUserRequest user) throws ConflictException {
        if (repository.existsByUsername(user.username)) {
            throw new ConflictException();
        }
        boolean locked = true;
        Role role = Role.MERCHANT;
        if(!repository.existsBy()) {
            locked = false;
            role = Role.ADMINISTRATOR;
        }
        var u = new User(user.username, pw.encode(user.password), user.name, locked, role);
        return new UserResponse(repository.save(u));
    }

    @GetMapping("/list")
    Stream<UserResponse> listUsers() {
        return repository.findByOrderById().stream().map(UserResponse::new);
    }

    @DeleteMapping("/user/{username}")
    DeleteResponse deleteUser(@PathVariable String username) throws NotFoundException {
        var user = repository.findUserByUsername(username).orElseThrow(NotFoundException::new);
        repository.delete(user);
        return new DeleteResponse(user);
    }

    @PutMapping("/role")
    UserResponse updateRole(@Valid @RequestBody UpdateRoleRequest request) throws NotFoundException {
        if (!(request.role == Role.MERCHANT || request.role == Role.SUPPORT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only change to MERCHANT or SUPPORT");
        }

        var user = repository.findUserByUsername(request.username()).orElseThrow(NotFoundException::new);
        if (user.role == request.role()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        user.role = request.role();

        return new UserResponse(repository.save(user));
    }

    @PutMapping("access")
    LockResponse lock(@Valid @RequestBody LockRequest lockRequest) throws NotFoundException {
        var user = repository.findUserByUsername(lockRequest.username()).orElseThrow(NotFoundException::new);
        if (user.role == Role.ADMINISTRATOR) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator cannot be locked");
        user.locked = lockRequest.operation == LockOperation.LOCK;
        return new LockResponse(repository.save(user));
    }

    record LockRequest(@NotNull String username, @NotNull LockOperation operation){}
    record UpdateRoleRequest(@NotNull String username, @NotNull Role role) {}
    record CreateUserRequest(@NotNull String name, @NotNull String username, @NotNull String password) {}
    record UserResponse(long id, String name, String username, Role role) {
        UserResponse(User user) {
            this(user.id, user.name, user.username, user.role);
        }
    }
    record DeleteResponse(String username, String status){
        DeleteResponse(User user) {
            this(user.username, "Deleted successfully!");
        }
    }
    record LockResponse(String status){
        LockResponse(User user) {
            this("User %s %s!".formatted(user.username, user.locked ? "locked" : "unlocked"));
        }
    }

}
