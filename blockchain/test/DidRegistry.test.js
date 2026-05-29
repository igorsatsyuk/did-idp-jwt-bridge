const { expect } = require("chai");
const { ethers } = require("hardhat");

// Uncompressed secp256k1 public key format: 0x04 prefix byte +
// x-coordinate (32 bytes) + y-coordinate (32 bytes) = 65 bytes total
// represented as a hex string: "0x" + 130 hex chars.
// No on-chain format validation — the contract stores any string;
// callers are responsible for providing valid EC keys.
const PK = {
  alice:   "0x04" + "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2" + "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
  bob:     "0x04" + "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3" + "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5",
  charlie: "0x04" + "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4" + "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6",
  dave:    "0x04" + "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5" + "f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1",
  eve:     "0x04" + "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6" + "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
  frank:   "0x04" + "f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1" + "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3",
};

describe("DidRegistry", function () {
  let registry;
  let owner;
  let other;

  beforeEach(async function () {
    [owner, other] = await ethers.getSigners();
    const DidRegistry = await ethers.getContractFactory("DidRegistry");
    registry = await DidRegistry.deploy();
    await registry.waitForDeployment();
  });

  it("should register a DID", async function () {
    await registry.registerDid("did:example:123", PK.alice);
    const [publicKey, status] = await registry.getDid("did:example:123");
    expect(publicKey).to.equal(PK.alice);
    expect(status).to.equal(0n);
  });

  it("should revoke a DID", async function () {
    await registry.registerDid("did:example:456", PK.bob);
    await registry.revokeDid("did:example:456");
    const [, status] = await registry.getDid("did:example:456");
    expect(status).to.equal(1n);
  });

  it("should not allow duplicate registration", async function () {
    await registry.registerDid("did:example:789", PK.charlie);
    await expect(
      registry.registerDid("did:example:789", PK.charlie)
    ).to.be.revertedWith("DID already registered");
  });

  it("should only allow owner to revoke", async function () {
    await registry.registerDid("did:example:abc", PK.dave);
    await expect(
      registry.connect(other).revokeDid("did:example:abc")
    ).to.be.revertedWith("Not the DID owner");
  });

  it("should update DID public key for owner", async function () {
    await registry.registerDid("did:example:key-rotation", PK.alice);

    await registry.updatePublicKey("did:example:key-rotation", PK.bob);

    const [publicKey] = await registry.getDid("did:example:key-rotation");
    expect(publicKey).to.equal(PK.bob);
  });

  it("should not allow non-owner to update DID public key", async function () {
    await registry.registerDid("did:example:key-rotation-owner", PK.alice);

    await expect(
      registry.connect(other).updatePublicKey("did:example:key-rotation-owner", PK.bob)
    ).to.be.revertedWith("Not the DID owner");
  });

  it("should not allow key update for revoked DID", async function () {
    await registry.registerDid("did:example:key-rotation-revoked", PK.alice);
    await registry.revokeDid("did:example:key-rotation-revoked");

    await expect(
      registry.updatePublicKey("did:example:key-rotation-revoked", PK.bob)
    ).to.be.revertedWith("DID is revoked");
  });

  it("isActive should return true for an active DID", async function () {
    await registry.registerDid("did:example:active", PK.eve);
    expect(await registry.isActive("did:example:active")).to.equal(true);
  });

  it("isActive should return false for a revoked DID", async function () {
    await registry.registerDid("did:example:revoked", PK.frank);
    await registry.revokeDid("did:example:revoked");
    expect(await registry.isActive("did:example:revoked")).to.equal(false);
  });

  it("isActive should return false for a non-existent DID", async function () {
    expect(await registry.isActive("did:example:nonexistent")).to.equal(false);
  });

  it("getDid should revert for non-existent DID", async function () {
    await expect(
      registry.getDid("did:example:missing")
    ).to.be.revertedWith("DID does not exist");
  });

  it("revokeDid should revert for non-existent DID", async function () {
    await expect(
      registry.revokeDid("did:example:missing")
    ).to.be.revertedWith("DID does not exist");
  });
});
