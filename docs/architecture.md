# Architecture

## Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Client (Browser / App)               │
└───────────────────┬─────────────────────────────────────┘
                    │ 1. DID + challenge + signature
                    ▼
┌───────────────────────────────┐
│        Auth Bridge Service    │  port 8082
│  - Resolves DID from Identity │
│  - Verifies ECDSA signature   │
│  - Issues JWT                 │
└──────────┬───────────┬────────┘
           │ 2. Get DID│         3. JWT
           ▼           ▼
┌──────────────────┐  ┌──────────────────┐
│ Identity Service │  │  Resource API    │  port 8083
│  port 8081       │  │  - JWT protected │
│  - Register DID  │  │  - /api/me       │
│  - Revoke DID    │  └──────────────────┘
└────────┬─────────┘
         │ Web3j
         ▼
┌─────────────────────┐
│  DidRegistry        │
│  Smart Contract     │  Ethereum / Hardhat
│  - registerDid()    │  port 8545
│  - revokeDid()      │
│  - getDid()         │
└─────────────────────┘
```

## Components

### DidRegistry (Smart Contract)
Solidity contract deployed on a private Ethereum network. Stores DID → public key mappings with active/revoked status.

### Identity Service
Spring Boot/WebFlux REST API. Exposes DID registration and lookup. Calls `DidRegistry` via Web3j.

### Auth Bridge Service
Accepts `POST /auth/token` with `{did, challenge, signature}`. Validates the DID is active, verifies the Ethereum-style ECDSA signature, and returns a JWT.

### Resource API
Standard JWT-protected Spring Boot/WebFlux API. Does not know about blockchain. Reads DID claims from JWT.

## Auth Flow (Sequence)

```
Client          Auth Bridge      Identity Service     Blockchain
  │                  │                  │                  │
  │─── challenge ───▶│                  │                  │
  │◀── nonce ────────│                  │                  │
  │                  │                  │                  │
  │ sign(nonce)      │                  │                  │
  │                  │                  │                  │
  │─── DID+sig ─────▶│                  │                  │
  │                  │── GET /did/{id} ▶│                  │
  │                  │                  │── getDid() ─────▶│
  │                  │                  │◀─ record ────────│
  │                  │◀── DidDocument ──│                  │
  │                  │                  │                  │
  │                  │  verify sig      │                  │
  │                  │  issue JWT       │                  │
  │◀─── JWT ─────────│                  │                  │
```
