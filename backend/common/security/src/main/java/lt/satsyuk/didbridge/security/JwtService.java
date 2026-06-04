package lt.satsyuk.didbridge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String did, Map<String, Object> extraClaims) {
        return generateToken(did, extraClaims, expirationMinutes);
    }

    public String generateToken(String did, Map<String, Object> extraClaims, long tokenExpirationMinutes) {
        if (tokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("tokenExpirationMinutes must be positive");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(did)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenExpirationMinutes, ChronoUnit.MINUTES)))
                .claims(extraClaims)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return parseSignedClaims(token).getPayload();
    }

    public Jws<Claims> parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception _) {
            return false;
        }
    }
}

