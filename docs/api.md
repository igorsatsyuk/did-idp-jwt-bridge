# API Reference

## Identity Service (`http://localhost:8081`)

### POST `/did/register`
Registers a new DID in `DidRegistry` and returns DID document.

Request:
```json
{
  "did": "did:ethr:0x1111111111111111111111111111111111111111",
  "publicKey": "0x04aabbcc..."
}
```

Response `201 Created`:
```json
{
  "did": "did:ethr:0x1111111111111111111111111111111111111111",
  "publicKey": "0x04aabbcc...",
  "status": "ACTIVE",
  "createdAt": "2026-05-28T16:00:00Z",
  "updatedAt": "2026-05-28T16:00:00Z"
}
```

Possible errors:
- `409 Conflict` — DID already registered.

### GET `/did/{did}`
Resolves DID document from blockchain.

Example:
```bash
curl "http://localhost:8081/did/did:ethr:0x1111111111111111111111111111111111111111"
```

Response `200 OK`:
```json
{
  "did": "did:ethr:0x1111111111111111111111111111111111111111",
  "publicKey": "0x04aabbcc...",
  "status": "ACTIVE",
  "createdAt": "2026-05-28T16:00:00Z",
  "updatedAt": "2026-05-28T16:00:00Z"
}
```

Possible errors:
- `404 Not Found` — DID does not exist.

### DELETE `/did/{did}/revoke`
Revokes DID status on-chain.

Example:
```bash
curl -X DELETE "http://localhost:8081/did/did:ethr:0x1111111111111111111111111111111111111111/revoke"
```

Response `204 No Content`

Possible errors:
- `404 Not Found` — DID does not exist.
- `403 Forbidden` — caller is not DID owner.

---

## Auth Bridge Service (`http://localhost:8082`)

### GET `/auth/challenge`
Returns one-time nonce challenge in the format `<instanceId>:<uuid>`.

Response `200 OK`:
```text
auth-bridge-service:3dcf3c45-fc87-4aac-9af7-82e92f2b34af
```

Possible errors:
- `429 Too Many Requests` — max active challenges reached.

### POST `/auth/token`
Validates challenge, resolves DID document, verifies ECDSA signature, issues JWT.

Request:
```json
{
  "did": "did:ethr:0x1111111111111111111111111111111111111111",
  "challenge": "auth-bridge-service:3dcf3c45-fc87-4aac-9af7-82e92f2b34af",
  "signature": "0x..."
}
```

Response `200 OK`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Possible errors:
- `401 Unauthorized` — challenge invalid/expired/replayed.
- `401 Unauthorized` — DID revoked.
- `5xx` — signature or upstream processing failure.

---

## Resource API (`http://localhost:8083`)

### GET `/api/me`
Returns JWT subject and claims. Requires bearer token.

Example:
```bash
curl "http://localhost:8083/api/me" \
  -H "Authorization: Bearer <jwt>"
```

Response `200 OK`:
```json
{
  "did": "did:ethr:0x1111111111111111111111111111111111111111",
  "claims": {
    "did": "did:ethr:0x1111111111111111111111111111111111111111",
    "sub": "did:ethr:0x1111111111111111111111111111111111111111",
    "iat": "2026-05-28T16:00:00Z",
    "exp": "2026-05-28T17:00:00Z"
  }
}
```

Possible errors:
- `401 Unauthorized` — missing, invalid, or expired JWT.

---

## DidRegistry Smart Contract

**Network**: Hardhat / private Ethereum (`:8545`)  
**ABI**: `backend/identity-service/src/main/resources/abi/DidRegistry.abi`  
**Java wrapper**: generated under `backend/identity-service/target/generated-sources/web3j/`

### `registerDid(string did, string publicKey)`
Registers DID and emits `DidRegistered`.

Reverts:
- `"DID already registered"`

### `revokeDid(string did)`
Revokes DID (owner-only) and emits `DidRevoked`.

Reverts:
- `"DID does not exist"`
- `"Not the DID owner"`

### `getDid(string did) -> (publicKey, status, createdAt, updatedAt, owner)`
Returns full DID record. `status`: `0 = ACTIVE`, `1 = REVOKED`.

Reverts:
- `"DID does not exist"`

### `isActive(string did) -> bool`
Returns `true` only for existing `ACTIVE` DID, otherwise `false`.
