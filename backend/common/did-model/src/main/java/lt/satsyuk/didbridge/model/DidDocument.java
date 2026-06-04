package lt.satsyuk.didbridge.model;

import java.time.Instant;

public record DidDocument(
        String did,
        String publicKey,
        DidStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

