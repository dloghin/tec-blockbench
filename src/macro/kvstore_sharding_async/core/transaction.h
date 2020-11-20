#ifndef YCSB_C_TRANSACTION_H_
#define YCSB_C_TRANSACTION_H_

#include <vector>
#include <unordered_map>
#include <iostream>
#include "core/db.h"
#include "db/hyperledger_kvdb.h"


using std::vector;
using std::unordered_map;
using std::string;
using ycsbc::DB;

#define STATE_NONE				0
#define STATE_SINGLE_SHARD		1
#define STATE_PREPARE			2
#define STATE_ABORT				3
#define STATE_COMMIT			4
#define STATE_DONE_SUCCESS		5
#define STATE_DONE_FAIL			6

class MultiShardTransaction {
private:
	int state;

public:
	vector<int> keyset;
	unordered_map<int, vector<string>> shardmap;		// map shard to txn keys
	vector<string> prepareTxns;
	vector<string> commitTxns;
	vector<DB::KVPair> values;
	long start_time;

	MultiShardTransaction(vector<string> keys, vector<DB::KVPair> &vals) {
		state = STATE_NONE;
		values = vals;
		start_time = 0.0;
	};

	void set_state(int new_state) {
		state = new_state;
	}

	int get_state() {
		return state;
	}

	int update_state(bool success, string txid) {
		switch (state) {
		case STATE_PREPARE:
			if (success) {
				bool found = false;
				size_t nFinished = 0;
				for (size_t i = 0; i < prepareTxns.size(); i++) {
					if (prepareTxns[i].compare(txid) == 0) {
						found = true;
						prepareTxns[i] = "";
						nFinished++;
						break;
					}
					else if (prepareTxns[i].compare("") == 0) {
						nFinished++;
					}
				}
				if (nFinished == prepareTxns.size()) {
					state = STATE_COMMIT;
				}
				else if (!found) {
					std::cout << "Strange: txid not found: " << txid << std::endl;
				}
			}
			else {
				state = STATE_ABORT;
			}
			break;
		case STATE_COMMIT:
			if (success) {
				bool found = false;
				size_t nFinished = 0;
				for (size_t i = 0; i < commitTxns.size(); i++) {
					if (commitTxns[i].compare(txid) == 0) {
						found = true;
						commitTxns[i] = "";
						nFinished++;
						break;
					}
					else if (commitTxns[i].compare("") == 0) {
						nFinished++;
					}
				}
				if (nFinished == commitTxns.size()) {
					state = STATE_DONE_SUCCESS;
				}
				else if (!found) {
					std::cout << "Strange: txid not found: " << txid << std::endl;
				}
			}
			else {
				state = STATE_DONE_FAIL;
			}
			break;
		}
		return state;
	}
};
#endif // YCSB_C_TRANSACTION_H_
