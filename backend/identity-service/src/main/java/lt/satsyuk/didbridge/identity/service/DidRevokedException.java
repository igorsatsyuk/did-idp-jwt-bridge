package lt.satsyuk.didbridge.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DidRevokedException extends RuntimeException {
    public DidRevokedException(String did, Throwable cause) {
        super("DID is revoked and cannot be modified: " + did, cause);
    }
}

