package lt.satsyuk.didbridge.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DidAlreadyRegisteredException extends RuntimeException {
    public DidAlreadyRegisteredException(String did, Throwable cause) {
        super("DID already registered: " + did, cause);
    }
}

