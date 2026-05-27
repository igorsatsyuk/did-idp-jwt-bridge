"use strict";

const assert = require("node:assert/strict");
const { describe, it, before, after } = require("node:test");

describe("deploy script", () => {
    let originalHre;
    let consoleLines;
    let origConsoleLog;
    let origConsoleError;

    before(() => {
        const deployedAddress = "0x1234567890123456789012345678901234567890";
        consoleLines = [];

        origConsoleLog = console.log;
        origConsoleError = console.error;
        console.log = (...args) => consoleLines.push(args.join(" "));
        console.error = () => {};

        const mockRegistry = {
            waitForDeployment: async () => {},
            getAddress: async () => deployedAddress,
        };
        const mockHre = {
            ethers: {
                getContractFactory: async (name) => {
                    assert.equal(name, "DidRegistry");
                    return { deploy: async () => mockRegistry };
                },
            },
        };

        originalHre = require.cache[require.resolve("hardhat")];
        require.cache[require.resolve("hardhat")] = {
            id: require.resolve("hardhat"),
            filename: require.resolve("hardhat"),
            loaded: true,
            exports: mockHre,
        };
    });

    after(() => {
        console.log = origConsoleLog;
        console.error = origConsoleError;

        if (originalHre) {
            require.cache[require.resolve("hardhat")] = originalHre;
        } else {
            delete require.cache[require.resolve("hardhat")];
        }
        delete require.cache[require.resolve("../scripts/deploy.js")];
    });

    it("calls getContractFactory, deploys and logs the address", async () => {
        delete require.cache[require.resolve("../scripts/deploy.js")];
        require("../scripts/deploy.js");

        // Allow the async main() to settle
        await new Promise((resolve) => setTimeout(resolve, 200));

        const output = consoleLines.join("\n");
        assert.ok(
            output.includes("0x1234567890123456789012345678901234567890"),
            `Expected deployed address in output. Got:\n${output}`
        );
    });
});
