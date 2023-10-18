package antifraud;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException extends Exception {
    BadRequestException() {
        super("Bad Request");
    }
}
