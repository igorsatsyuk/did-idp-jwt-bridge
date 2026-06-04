package lt.satsyuk.didbridge.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DidOwnershipException extends RuntimeException {
    public DidOwnershipException(String did, Throwable cause) {
        super("Caller is not the owner of DID: " + did, cause);
    }
}

