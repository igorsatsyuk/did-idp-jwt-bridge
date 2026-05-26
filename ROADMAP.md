# Roadmap

## Phase 1 — Base Infrastructure

- [x] Repository initialization and Maven multi-module structure
- [ ] Blockchain environment (Hardhat + Ganache node on port 8545)
- [ ] DidRegistry smart contract (register, revoke, isActive)

## Phase 2 — Backend Services

- [ ] Identity Service: Web3j integration with DidRegistry contract
- [ ] Auth Bridge: ECDSA signature verification (Ethereum signed message)
- [ ] Auth Bridge: challenge nonce management (TTL, replay protection)
- [ ] Resource API: JWT validation wired up with JwtService

## Phase 3 — Frontend

- [ ] Angular DID UI scaffold
- [ ] Register DID page (generate key pair in-browser with ethers.js)
- [ ] Auth flow page (challenge → sign → get JWT)
- [ ] Call protected Resource API and display claims

## Phase 4 — Enhancements

- [ ] DID revocation — full flow (contract + API + UI)
- [ ] Architecture diagrams (C4 context, sequence)
- [ ] Key rotation (`updatePublicKey` in DidRegistry)
- [ ] JWT refresh token mechanism
- [ ] Security hardening (rate limiting, nonce expiration)
- [ ] Docker Compose full stack with health checks
- [ ] Kubernetes manifests
