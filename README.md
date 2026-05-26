# Decentralized Identity Provider (DID IdP) + JWT Bridge

A system that uses a private blockchain as the root of trust for decentralized identities (DID), while allowing standard services to continue using familiar JWT tokens.

> **Goal:** Demonstrate Senior/Architect-level architecture: decentralized identity registry, cryptographic signatures, DID-to-JWT bridge, multi-service design.

## Architecture

```
Client ──► Auth Bridge ──► Identity Service ──► Blockchain (DidRegistry)
                │
                └──► JWT ──► Resource API
```

See [docs/architecture.md](docs/architecture.md) for the full diagram and sequence.

## Tech Stack

| Layer        | Technology                                |
|--------------|-------------------------------------------|
| Backend      | Java 25, Spring Boot 4.0.6, Spring WebFlux |
| Security     | Spring Security, JJWT, Web3j (ECDSA)     |
| Blockchain   | Hardhat, Solidity 0.8.24, Ganache         |
| Frontend     | Angular (coming in Phase 3)               |
| Deploy       | Docker Compose                            |

## Quick Start

### Prerequisites
- Java 25+
- Maven 3.9+
- Node.js 20+ (for blockchain)
- Docker & Docker Compose

### 1. Start the blockchain node
```bash
cd blockchain
npm install
npm run node          # starts Hardhat node on port 8545
# In another terminal:
npm run deploy:local  # deploys DidRegistry, prints contract address
```

### 2. Set environment variables
```bash
cp deploy/.env.example deploy/.env
# Edit deploy/.env with the contract address from step 1
```

### 3. Run backend services
```bash
cd backend
mvn -pl identity-service -am spring-boot:run &
mvn -pl auth-bridge-service -am spring-boot:run &
mvn -pl resource-api -am spring-boot:run &
```

### 4. Try the auth flow
```bash
# Register a DID
curl -X POST http://localhost:8081/did/register \
  -H "Content-Type: application/json" \
  -d '{"did":"did:example:alice","publicKey":"0x04..."}'

# Get a challenge
curl http://localhost:8082/auth/challenge

# Authenticate (get JWT)
curl -X POST http://localhost:8082/auth/token \
  -H "Content-Type: application/json" \
  -d '{"did":"did:example:alice","challenge":"<from above>","signature":"0x..."}'

# Call protected API
curl http://localhost:8083/api/me \
  -H "Authorization: Bearer <jwt>"
```

## Project Structure

```
did-idp-jwt-bridge/
├─ backend/
│  ├─ common/
│  │  ├─ did-model/          # Shared domain model (DidDocument, DidStatus)
│  │  └─ security/           # Shared JWT utilities
│  ├─ identity-service/      # DID registration REST API (port 8081)
│  ├─ auth-bridge-service/   # DID auth → JWT issuer (port 8082)
│  └─ resource-api/          # JWT-protected example API (port 8083)
├─ blockchain/
│  ├─ contracts/DidRegistry.sol
│  └─ scripts/deploy.js
├─ deploy/
│  └─ docker-compose.yml
└─ docs/
   ├─ architecture.md
   └─ api.md
```

## Roadmap

See [ROADMAP.md](ROADMAP.md) for detailed phases and GitHub Issues.

## License

MIT
