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
│  - isActive()       │
└─────────────────────┘
```

## Auth Flow (Sequence)

```
Client        Auth Bridge    Identity Service   Blockchain
  │                │                │                │
  │── challenge ──▶│                │                │
  │◀── nonce ──────│                │                │
  │                │                │                │
  │ sign(nonce)    │                │                │
  │── DID+sig ────▶│                │                │
  │                │── GET /did ───▶│                │
  │                │                │── getDid() ───▶│
  │                │                │◀── record ─────│
  │                │◀── DidDocument─│                │
  │                │  verify sig    │                │
  │                │  issue JWT     │                │
  │◀── JWT ────────│                │                │
```
