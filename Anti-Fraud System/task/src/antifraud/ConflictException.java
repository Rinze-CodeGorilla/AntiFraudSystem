package antifraud;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException extends Exception {
    ConflictException() {
        super("User exists");
    }
}
