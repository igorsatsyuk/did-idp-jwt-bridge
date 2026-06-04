package lt.satsyuk.didbridge.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DidNotFoundException extends RuntimeException {
    public DidNotFoundException(String did) {
        super("DID not found: " + did);
    }

    public DidNotFoundException(String did, Throwable cause) {
        super("DID not found: " + did, cause);
    }
}

