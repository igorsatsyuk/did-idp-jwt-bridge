// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/// @title DidRegistry
/// @notice Stores and manages Decentralized Identifiers (DIDs) and their public keys.
contract DidRegistry {

    enum Status { Active, Revoked }

    struct DidRecord {
        string publicKey;
        Status status;
        uint256 createdAt;
        uint256 updatedAt;
        address owner;
    }

    mapping(string => DidRecord) private records;

    event DidRegistered(string indexed did, address indexed owner, uint256 timestamp);
    event DidRevoked(string indexed did, address indexed owner, uint256 timestamp);
    event PublicKeyUpdated(string indexed did, address indexed owner, uint256 timestamp);

    modifier onlyOwner(string calldata did) {
        require(records[did].owner == msg.sender, "Not the DID owner");
        _;
    }

    modifier didExists(string calldata did) {
        require(records[did].createdAt != 0, "DID does not exist");
        _;
    }

    function registerDid(string calldata did, string calldata publicKey) external {
        require(records[did].createdAt == 0, "DID already registered");
        records[did] = DidRecord({
            publicKey: publicKey,
            status: Status.Active,
            createdAt: block.timestamp,
            updatedAt: block.timestamp,
            owner: msg.sender
        });
        emit DidRegistered(did, msg.sender, block.timestamp);
    }

    function revokeDid(string calldata did) external didExists(did) onlyOwner(did) {
        records[did].status = Status.Revoked;
        records[did].updatedAt = block.timestamp;
        emit DidRevoked(did, msg.sender, block.timestamp);
    }

    function updatePublicKey(
        string calldata did,
        string calldata newPublicKey
    ) external didExists(did) onlyOwner(did) {
        require(records[did].status == Status.Active, "DID is revoked");
        records[did].publicKey = newPublicKey;
        records[did].updatedAt = block.timestamp;
        emit PublicKeyUpdated(did, msg.sender, block.timestamp);
    }

    function getDid(string calldata did) external view didExists(did) returns (
        string memory publicKey,
        Status status,
        uint256 createdAt,
        uint256 updatedAt,
        address owner
    ) {
        DidRecord storage rec = records[did];
        return (rec.publicKey, rec.status, rec.createdAt, rec.updatedAt, rec.owner);
    }

    function isActive(string calldata did) external view returns (bool) {
        return records[did].status == Status.Active && records[did].createdAt != 0;
    }
}
