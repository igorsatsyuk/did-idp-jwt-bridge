# Decentralized Identity Provider (DID IdP) + JWT Bridge

A system that uses a private blockchain as the root of trust for decentralized identities (DID), while standard services continue using familiar JWT tokens.

> **Goal:** Demonstrate Senior/Architect-level design — decentralized identity, cryptographic proofs, DID-to-JWT bridge, microservices.

## Architecture

```
Client ──► Auth Bridge (8082) ──► Identity Service (8081) ──► Blockchain :8545
                │                                                 DidRegistry.sol
                └──► JWT ──► Resource API (8083)
```

Full diagram: [docs/architecture.md](docs/architecture.md)

## Tech Stack

| Layer      | Technology                                     |
|------------|------------------------------------------------|
| Backend    | Java 25, Spring Boot 4.0.6, Spring WebFlux     |
| Security   | Spring Security, JJWT 0.12.6, Web3j (ECDSA)   |
| Blockchain | Hardhat, Solidity 0.8.24, Ganache              |
| Frontend   | Angular (Phase 3)                              |
| Deploy     | Docker Compose                                 |

## Quick Start

### Prerequisites
- Java 25+, Maven 3.9+
- Node.js 20+
- Docker & Docker Compose

### 1. Start blockchain
```bash
cd blockchain && npm install && npm run node
# In another terminal:
npm run deploy:local   # prints DID_REGISTRY_ADDRESS
```

### 2. Configure environment
```bash
cp deploy/.env.example deploy/.env
# Set DID_REGISTRY_ADDRESS from previous step
```

### 3. Run backend services
```bash
cd backend
mvn -pl identity-service -am spring-boot:run &
mvn -pl auth-bridge-service -am spring-boot:run &
mvn -pl resource-api -am spring-boot:run &
```

### 4. Auth flow (curl)
```bash
# Register DID
curl -X POST http://localhost:8081/did/register \
  -H "Content-Type: application/json" \
  -d '{"did":"did:example:alice","publicKey":"0x04..."}'

# Get challenge
curl http://localhost:8082/auth/challenge

# Get JWT
curl -X POST http://localhost:8082/auth/token \
  -H "Content-Type: application/json" \
  -d '{"did":"did:example:alice","challenge":"<uuid>","signature":"0x..."}'

# Call protected API
curl http://localhost:8083/api/me -H "Authorization: Bearer <jwt>"
```

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## License

MIT
