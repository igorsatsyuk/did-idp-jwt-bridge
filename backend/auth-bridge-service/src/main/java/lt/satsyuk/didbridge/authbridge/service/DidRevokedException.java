package lt.satsyuk.didbridge.authbridge.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class DidRevokedException extends RuntimeException {
    public DidRevokedException(String did) {
        super("DID is revoked: " + did);
    }
}

