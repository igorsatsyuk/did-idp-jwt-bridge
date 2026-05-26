# Roadmap

## Phase 1 — Base Infrastructure

- [x] Repository initialization and Maven structure
- [ ] Blockchain environment (Hardhat + Ganache)
- [ ] DidRegistry smart contract (register, revoke, read)

## Phase 2 — Backend Services

- [ ] Identity Service MVP (`POST /did/register`, `GET /did/{id}`, `DELETE /did/{id}/revoke`)
- [ ] Web3j integration with DidRegistry contract
- [ ] Auth Bridge MVP (`POST /auth/token`, `GET /auth/challenge`)
- [ ] ECDSA signature verification
- [ ] Resource API MVP (`GET /api/me` with JWT)

## Phase 3 — Frontend

- [ ] Angular DID UI
  - Register DID form
  - View DID status
  - Generate challenge, sign, get JWT
  - Call protected Resource API
- [ ] Integration with Resource API from UI

## Phase 4 — Enhancements

- [ ] DID revocation UI + contract integration
- [ ] Architecture diagrams (sequence, context)
- [ ] Key rotation (update public key for a DID)
- [ ] JWT refresh mechanism
- [ ] Security hardening (rate limiting, nonce expiration)
- [ ] Kubernetes manifests
