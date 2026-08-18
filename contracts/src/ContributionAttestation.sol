// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract ContributionAttestation {
    error CertificateAlreadyExists(bytes32 certificateId);
    error CertificateNotFound(bytes32 certificateId);
    error UnauthorizedIssuer(address caller);
    error CertificateAlreadyRevoked(bytes32 certificateId);

    struct Attestation {
        bytes32 certificateHash;
        address issuer;
        address subject;
        uint64 issuedAt;
        uint64 revokedAt;
    }

    mapping(bytes32 certificateId => Attestation) private attestations;

    event CertificateIssued(
        bytes32 indexed certificateId,
        bytes32 certificateHash,
        address indexed issuer,
        address indexed subject
    );
    event CertificateRevoked(bytes32 indexed certificateId, address indexed issuer, uint64 revokedAt);

    function issue(bytes32 certificateId, bytes32 certificateHash, address subject) external {
        if (attestations[certificateId].issuedAt != 0) {
            revert CertificateAlreadyExists(certificateId);
        }
        uint64 issuedAt = uint64(block.timestamp);
        attestations[certificateId] = Attestation({
            certificateHash: certificateHash,
            issuer: msg.sender,
            subject: subject,
            issuedAt: issuedAt,
            revokedAt: 0
        });
        emit CertificateIssued(certificateId, certificateHash, msg.sender, subject);
    }

    function revoke(bytes32 certificateId) external {
        Attestation storage attestation = attestations[certificateId];
        if (attestation.issuedAt == 0) revert CertificateNotFound(certificateId);
        if (attestation.issuer != msg.sender) revert UnauthorizedIssuer(msg.sender);
        if (attestation.revokedAt != 0) revert CertificateAlreadyRevoked(certificateId);
        attestation.revokedAt = uint64(block.timestamp);
        emit CertificateRevoked(certificateId, msg.sender, attestation.revokedAt);
    }

    function get(bytes32 certificateId) external view returns (Attestation memory) {
        Attestation memory attestation = attestations[certificateId];
        if (attestation.issuedAt == 0) revert CertificateNotFound(certificateId);
        return attestation;
    }

    function exists(bytes32 certificateId) external view returns (bool) {
        return attestations[certificateId].issuedAt != 0;
    }
}
