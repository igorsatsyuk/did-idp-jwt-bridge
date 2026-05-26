# API Reference

## Identity Service (port 8081)

### Register DID
`POST /did/register`
```json
{
  "did": "did:example:123abc",
  "publicKey": "0x04..."
}
```
Response `201 Created`:
```json
{
  "did": "did:example:123abc",
  "publicKey": "0x04...",
  "status": "ACTIVE",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

### Get DID
`GET /did/{did}`

### Revoke DID
`DELETE /did/{did}/revoke`

---

## Auth Bridge Service (port 8082)

### Get Challenge
`GET /auth/challenge`
Returns a random UUID challenge string.

### Authenticate
`POST /auth/token`
```json
{
  "did": "did:example:123abc",
  "challenge": "550e8400-e29b-41d4-a716-446655440000",
  "signature": "0x..."
}
```
Response `200 OK`:
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## Resource API (port 8083)

### Get Current Identity
`GET /api/me`
Headers: `Authorization: Bearer <jwt>`

Response `200 OK`:
```json
{
  "did": "did:example:123abc",
  "claims": { ... }
}
```
