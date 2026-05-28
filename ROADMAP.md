# Roadmap

## Phase 1 — Base Infrastructure

- [x] Repository initialization and Maven multi-module structure
- [x] Blockchain environment (Hardhat + Ganache node on port 8545)
- [x] DidRegistry smart contract (register, revoke, isActive)

## Phase 2 — Backend Services

- [x] Identity Service: Web3j integration with DidRegistry contract
- [x] Auth Bridge: ECDSA signature verification (Ethereum signed message)
- [x] Auth Bridge: challenge nonce management (TTL, replay protection)
- [x] Resource API: JWT validation wired up with JwtService

## Phase 3 — Frontend

- [x] Angular DID UI scaffold
- [x] Register DID page (generate key pair in-browser with ethers.js)
- [x] Auth flow page (challenge → sign → get JWT)
- [x] Call protected Resource API and display claims

## Phase 4 — Enhancements

- [ ] DID revocation — full flow (contract + API + UI)
- [ ] Architecture diagrams (C4 context, sequence)
- [ ] Key rotation (`updatePublicKey` in DidRegistry)
- [ ] JWT refresh token mechanism
- [ ] Security hardening (rate limiting, nonce expiration)
- [ ] Docker Compose full stack with health checks
- [ ] Kubernetes manifests
