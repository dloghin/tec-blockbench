pragma solidity ^0.5.0;

contract BDVFL {
    uint64 globalTaskId = 0;

    struct BDVFLTask {
        uint64 TaskId;
        string ModelName;
        string ModelHash;
        uint64 Rounds;
        address AggregatorAddr;
        address[] NodesAddrs;
        uint256 NodesAddrsLen;
    }

    mapping(uint64 => BDVFLTask) private tasks;
    mapping(string => string) private partialParamsHashes;
    mapping(string => string) private partialModelHashes;

    event TaskIdEvent(uint64 indexed _taskId);

    function CreateBDVFLTask(
        string memory modelName,
        string memory modelHash,
        uint64 rounds,
        address aggr,
        address[] memory addrs
    ) public {
        tasks[globalTaskId] = BDVFLTask({
            TaskId: globalTaskId,
            ModelName: modelName,
            ModelHash: modelHash,
            Rounds: rounds,
            AggregatorAddr: aggr,
            NodesAddrs: addrs,
            NodesAddrsLen: addrs.length
        });
        emit TaskIdEvent(globalTaskId);
        globalTaskId++;
    }

    function GetInitialModelHash(uint64 taskId)
        public
        view
        returns (string memory)
    {
        if (taskId >= globalTaskId) return "";
        return tasks[taskId].ModelHash;
    }

    function SetPartialParamsHash(
        uint64 taskId,
        uint64 round,
        string memory paramsHash
    ) public returns (uint64) {
        if (taskId >= globalTaskId) return 1;
        BDVFLTask memory task = tasks[taskId];
        bool found = false;
        for (uint256 i = 0; i < task.NodesAddrsLen; i++) {
            if (msg.sender == task.NodesAddrs[i]) {
                found = true;
                break;
            }
        }
        if (!found) {
            return 1;
        }
        string memory key = concatStrings(
            concatStrings(toString(taskId), toAsciiString(msg.sender)),
            toString(round)
        );
        partialParamsHashes[key] = paramsHash;
        return 0;
    }

    function GetPartialParamsHash(
        uint64 taskId,
        uint64 round,
        address node
    ) public view returns (string memory) {
        if (taskId >= globalTaskId) return "";
        string memory key = concatStrings(
            concatStrings(toString(taskId), toAsciiString(node)),
            toString(round)
        );
        return partialParamsHashes[key];
    }

    function SetPartialModelHash(
        uint64 taskId,
        uint64 round,
        string memory modelHash
    ) public returns (uint64) {
        if (taskId >= globalTaskId) return 1;
        BDVFLTask memory task = tasks[taskId];
        if (msg.sender != task.AggregatorAddr) {
            return 1;
        }
        string memory key = concatStrings(
            concatStrings("model_", toString(taskId)),
            concatStrings("_", toString(round))
        );
        partialModelHashes[key] = modelHash;
        return 0;
    }

    function GetPartialModelHash(uint64 taskId, uint64 round)
        public
        view
        returns (string memory)
    {
        if (taskId >= globalTaskId) return "";
        string memory key = concatStrings(
            concatStrings("model_", toString(taskId)),
            concatStrings("_", toString(round))
        );
        return partialModelHashes[key];
    }

    function toAsciiString(address x) internal pure returns (string memory) {
        bytes memory s = new bytes(40);
        for (uint256 i = 0; i < 20; i++) {
            bytes1 b = bytes1(uint8(uint256(uint160(x)) / (2**(8 * (19 - i)))));
            bytes1 hi = bytes1(uint8(b) / 16);
            bytes1 lo = bytes1(uint8(b) - 16 * uint8(hi));
            s[2 * i] = char(hi);
            s[2 * i + 1] = char(lo);
        }
        return string(s);
    }

    function char(bytes1 b) internal pure returns (bytes1 c) {
        if (uint8(b) < 10) return bytes1(uint8(b) + 0x30);
        else return bytes1(uint8(b) + 0x57);
    }

    function toString(uint256 value) internal pure returns (string memory) {
        bytes16 HEX_DIGITS = "0123456789abcdef";
        uint256 length = 257;
        string memory buffer = new string(length);
        uint256 ptr;
        /// @solidity memory-safe-assembly
        assembly {
            ptr := add(buffer, add(32, length))
        }
        while (true) {
            ptr--;
            /// @solidity memory-safe-assembly
            assembly {
                mstore8(ptr, byte(mod(value, 10), HEX_DIGITS))
            }
            value /= 10;
            if (value == 0) break;
        }
        return buffer;
    }

    function concatStrings(string memory _a, string memory _b)
        internal
        pure
        returns (string memory)
    {
        bytes memory bytes_a = bytes(_a);
        bytes memory bytes_b = bytes(_b);
        string memory ab = new string(bytes_a.length + bytes_b.length);
        bytes memory bytes_ab = bytes(ab);

        uint256 k = 0;
        for (uint256 i = 0; i < bytes_a.length; i++) bytes_ab[k++] = bytes_a[i];
        for (uint256 i = 0; i < bytes_b.length; i++) bytes_ab[k++] = bytes_b[i];

        return string(bytes_ab);
    }
}
