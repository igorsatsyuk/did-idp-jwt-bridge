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
