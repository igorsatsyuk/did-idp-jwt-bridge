# API Reference

## Identity Service — port 8081

### POST /did/register
Register a new DID.
```json
{ "did": "did:example:alice", "publicKey": "0x04..." }
```
Response 201:
```json
{ "did": "did:example:alice", "publicKey": "0x04...", "status": "ACTIVE", "createdAt": "...", "updatedAt": "..." }
```

### GET /did/{did}
Resolve a DID. Returns 404 if not found.

### DELETE /did/{did}/revoke
Revoke a DID. Returns 204.

---

## Auth Bridge Service — port 8082

### GET /auth/challenge
Returns a one-time challenge UUID string.

### POST /auth/token
```json
{ "did": "did:example:alice", "challenge": "uuid", "signature": "0x..." }
```
Response 200:
```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
```

---

## Resource API — port 8083

### GET /api/me
Requires `Authorization: Bearer <jwt>`.

Response 200:
```json
{ "did": "did:example:alice", "claims": { ... } }
```

---

## DidRegistry Smart Contract

**Network**: Hardhat / private Ethereum (port 8545)  
**ABI**: `backend/identity-service/src/main/resources/abi/DidRegistry.abi`  
**Java wrapper**: auto-generated at `backend/identity-service/target/generated-sources/web3j/com/didbridge/identity/contract/DidRegistry.java`

### registerDid(string did, string publicKey)
Registers a new DID on-chain. Emits `DidRegistered`.

| Param | Type | Description |
|-------|------|-------------|
| `did` | string | Fully-qualified DID string, e.g. `did:example:alice` |
| `publicKey` | string | Hex-encoded secp256k1 public key, e.g. `0x04...` |

Reverts with `"DID already registered"` if the DID exists.

### revokeDid(string did)
Revokes an existing DID. Only callable by the DID owner. Emits `DidRevoked`.

Reverts with `"DID does not exist"` or `"Not the DID owner"`.

### getDid(string did) → (publicKey, status, createdAt, updatedAt, owner)
Returns full DID record. `status`: `0 = Active`, `1 = Revoked`.

Reverts with `"DID does not exist"`.

### isActive(string did) → bool
Returns `true` if the DID exists and has `Active` status. Returns `false` for non-existent or revoked DIDs (no revert).

### Events

| Event | Params |
|-------|--------|
| `DidRegistered` | `string indexed did`, `address indexed owner`, `uint256 timestamp` |
| `DidRevoked` | `string indexed did`, `address indexed owner`, `uint256 timestamp` |
