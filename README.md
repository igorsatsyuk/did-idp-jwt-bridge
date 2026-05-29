# Decentralized Identity Provider (DID IdP) + JWT Bridge

A system that uses a private blockchain as the root of trust for decentralized identities (DID), while standard services continue using familiar JWT tokens.

## Architecture

![C4 context diagram](docs/diagrams/c4-context.png)

Full diagram: [docs/architecture.md](docs/architecture.md)

## Tech Stack

| Layer      | Technology                           |
|------------|--------------------------------------|
| Backend    | Java 25, Spring Boot 4.0.6, Spring WebFlux |
| Security   | Spring Security, JJWT 0.12.6, Web3j (ECDSA) |
| Blockchain | Hardhat, Solidity 0.8.24, Ganache    |
| Frontend   | Angular                              |
| Deploy     | Docker Compose                       |

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

### 4. Run frontend (Angular DID UI)
```bash
cd frontend/did-ui
npm install
npm start
```

### 5. Auth flow (curl)
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
  -d '{"did":"did:example:alice","challenge":"<challenge-from-/auth/challenge>","signature":"0x..."}'

# Call protected API
curl http://localhost:8083/api/me -H "Authorization: Bearer <jwt>"
```

> `auth-bridge-service` keeps active challenges in-memory. In multi-instance deployments, configure sticky routing for `/auth/challenge` and `/auth/token` to the same instance (or use a shared challenge store).

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## License

MIT
