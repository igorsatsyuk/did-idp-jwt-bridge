# Blockchain — DidRegistry

Local private Ethereum network using Hardhat.

## Prerequisites
- Node.js 20+ (`node --version`)
- npm 10+

## Setup

```bash
cd blockchain
npm install
```

## Commands

| Command | Description |
|---------|-------------|
| `npm run node` | Start Hardhat node on `http://localhost:8545` (chainId 1337) |
| `npm run compile` | Compile Solidity contracts |
| `npm test` | Run contract tests (4 tests) |
| `npm run deploy:local` | Deploy DidRegistry to local node |

## Quick Start

### 1. Start the node (keep this terminal open)
```bash
npm run node
```

### 2. Deploy DidRegistry (in a new terminal)
```bash
npm run deploy:local
```

Output example:
```
DidRegistry deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3

Add to your .env:
DID_REGISTRY_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
```

### 3. Copy the address to `deploy/.env`
```bash
cp ../deploy/.env.example ../deploy/.env
# Set DID_REGISTRY_ADDRESS=<address from step 2>
```

## Network Config

- **RPC URL:** `http://localhost:8545`
- **Chain ID:** `1337`
- **Accounts:** 5 pre-funded accounts with 1000 ETH each

## Contract: DidRegistry

Located at `contracts/DidRegistry.sol`.

### Functions
- `registerDid(did, publicKey)` — Register a new DID
- `revokeDid(did)` — Revoke a DID (owner only)
- `getDid(did)` — Get DID record
- `isActive(did)` — Check if DID is active
