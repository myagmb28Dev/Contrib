// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ContributionAttestation} from "../src/ContributionAttestation.sol";

contract ContributionAttestationTest {
    ContributionAttestation private attestation;
    bytes32 private constant ID = keccak256("certificate-1");
    bytes32 private constant HASH = keccak256("payload-1");
    address private constant SUBJECT = address(0xBEEF);

    constructor() {
        attestation = new ContributionAttestation();
    }

    function testIssueAndGet() external {
        attestation.issue(ID, HASH, SUBJECT);
        ContributionAttestation.Attestation memory stored = attestation.get(ID);
        require(stored.certificateHash == HASH, "hash mismatch");
        require(stored.issuer == address(this), "issuer mismatch");
        require(stored.subject == SUBJECT, "subject mismatch");
        require(stored.issuedAt > 0, "issuedAt missing");
        require(stored.revokedAt == 0, "unexpected revocation");
    }

    function testRevoke() external {
        bytes32 id = keccak256("certificate-2");
        attestation.issue(id, HASH, SUBJECT);
        attestation.revoke(id);
        require(attestation.get(id).revokedAt > 0, "revocation missing");
    }

    function testDuplicateIssueReverts() external {
        bytes32 id = keccak256("certificate-3");
        attestation.issue(id, HASH, SUBJECT);
        try attestation.issue(id, HASH, SUBJECT) {
            revert("duplicate issue should revert");
        } catch {
            return;
        }
    }
}
