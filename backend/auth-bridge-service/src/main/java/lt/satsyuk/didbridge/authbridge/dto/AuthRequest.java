package lt.satsyuk.didbridge.authbridge.dto;

public record AuthRequest(String did, String challenge, String signature) {}

