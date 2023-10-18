package antifraud;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnprocessableEntityException extends Exception {
    UnprocessableEntityException() {
        super("Unprocessable Entity");
    }
}
