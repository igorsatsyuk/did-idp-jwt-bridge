"use strict";

const assert = require("node:assert/strict");
const { describe, it } = require("node:test");
const { main } = require("../scripts/deploy.js");

describe("deploy script", { concurrency: false }, () => {
    it("deploys and logs contract address without errors", async () => {
        const deployedAddress = "0x1234567890123456789012345678901234567890";
        const consoleLines = [];
        const errorLines = [];

        const mockRuntime = {
            ethers: {
                getContractFactory: async (name) => {
                    assert.equal(name, "DidRegistry");
                    return {
                        deploy: async () => ({
                            waitForDeployment: async () => {},
                            getAddress: async () => deployedAddress,
                        }),
                    };
                },
            },
        };

        const logger = {
            log: (...args) => consoleLines.push(args.join(" ")),
            error: (...args) => errorLines.push(args.join(" ")),
        };

        const returnedAddress = await main(mockRuntime, logger);

        assert.equal(returnedAddress, deployedAddress);
        assert.equal(
            errorLines.length,
            0,
            `Unexpected error logs:\n${errorLines.join("\n")}`
        );

        const output = consoleLines.join("\n");
        assert.ok(
            output.includes(deployedAddress),
            `Expected deployed address in output. Got:\n${output}`
        );
    });
});
