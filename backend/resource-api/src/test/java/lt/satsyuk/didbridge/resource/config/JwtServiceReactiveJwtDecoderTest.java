package lt.satsyuk.didbridge.resource.config;

import lt.satsyuk.didbridge.security.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceReactiveJwtDecoderTest {

    private static final String SECRET = "change-me-in-production-this-must-be-at-least-32-chars";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void decode_returnsJwtWithClaims_forValidToken() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 60);
        JwtServiceReactiveJwtDecoder decoder = new JwtServiceReactiveJwtDecoder(jwtService);
        String token = jwtService.generateToken("did:example:alice", Map.of("role", "user"));

        Jwt jwt = decoder.decode(token).block();
        String algFromTokenHeader = extractHeader(token).get("alg").toString();

        assertThat(jwt.getSubject()).isEqualTo("did:example:alice");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("user");
        assertThat(jwt.getHeaders()).containsEntry("alg", algFromTokenHeader);
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    @Test
    void decode_throwsJwtException_forInvalidToken() {
        JwtService jwtService = new JwtService(SECRET, 60);
        JwtServiceReactiveJwtDecoder decoder = new JwtServiceReactiveJwtDecoder(jwtService);
        var decodedToken = decoder.decode("not-a-jwt");

        assertThatThrownBy(decodedToken::block)
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void decode_throwsJwtException_whenSubjectIsBlank() {
        JwtService jwtService = new JwtService(SECRET, 60);
        JwtServiceReactiveJwtDecoder decoder = new JwtServiceReactiveJwtDecoder(jwtService);
        String tokenWithBlankSubject = jwtService.generateToken("", Map.of("role", "user"));
        var decodedToken = decoder.decode(tokenWithBlankSubject);

        assertThatThrownBy(decodedToken::block)
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("JWT subject (did) is required");
    }

    private Map<String, Object> extractHeader(String token) throws Exception {
        String[] parts = token.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readValue(headerJson, new TypeReference<>() {});
    }
}

