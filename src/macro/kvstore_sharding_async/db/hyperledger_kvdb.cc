#include "db/hyperledger_kvdb.h"
#include "db/db_utils.h"
#include "core/client.h"
#include <restclient-cpp/restclient.h>
#include <string>
#include <vector>
#include <mutex>
#include <iostream>
#include <fstream>

using std::string;
using std::vector;
using std::ifstream;
using std::ofstream;
using namespace RestClient;

const string CHAIN_END_POINT = "/chain";
const string BLOCK_END_POINT = "/chain/blocks";

namespace ycsbc {

HyperLedgerKVDB::HyperLedgerKVDB(const string path, const string &endpoint, int nshards, int max_nr_keys,
		int client_index, int clients_per_shard) : post_count_(0) {
  deploy(path, endpoint, nshards, max_nr_keys, client_index, clients_per_shard); 
}

void HyperLedgerKVDB::deploy(const string path, const string &endpoint, int nshards, int max_nr_keys, int
client_index, int clients_per_shard) {
  nshards_ = nshards;
  endpoints_ = split(endpoint,';');
  max_nr_keys_ = max_nr_keys;
  assert(((size_t)nshards_) == endpoints_.size());

  /*
  // write to only client_per_shard
  for (int i=0; i<endpoints_.size(); i++) {
    if (client_index < (nshards*clients_per_shard) && (client_index/clients_per_shard)==i) {
      std::vector<std::string> args;
      Response r = post(endpoints_[i], REQUEST_HEADERS, compose_deploy(path, args)); 
      chaincode_name_ = get_json_field(r.body, "message");
    }
    if (client_index==0) {
      ofstream of("/users/dinhtta/blockbench/chaincode_0");
      of << chaincode_name_;
      of.close(); 
    }
  }
  */
  // we can choose to read from file
    ifstream chaincodeFile(path);
    getline(chaincodeFile, chaincode_name_);
    cout << "Read chaincode from file: " << chaincode_name_ << endl;
  /*
  for (int i=0; i<endpoints_.size(); i++) {
    if ((client_index/clients_per_shard)==i) {
      std::vector<std::string> args;
      Response r = post(endpoints_[i], REQUEST_HEADERS, compose_deploy(path, args)); 
      chaincode_name_ = get_json_field(r.body, "message");
    }
  }
  */
  cout << "Deploy chaincode " << chaincode_name_ << endl; 
  utils::sleep(5);
}
// ignore table
// ignore field
// read value indicated by a key
int HyperLedgerKVDB::Read(const string &table, const string &key,
                          const vector<string> *fields,
                          vector<DB::KVPair> &result) {
  vector<string> args;
  args.push_back(key); 
  Response r = post(endpoints_[0], REQUEST_HEADERS, 
                compose_invoke(chaincode_name_, "read", args));  
  return DB::kOK;
}


string HyperLedgerKVDB::update_single_key(const string &table, const string &key,
                            vector<DB::KVPair> &values, string endpoint) {
  string val = "";
  for (auto v : values) {
    val += v.first + "=" + v.second + " ";
  }
  for (auto &x : val) {
    if (x == '\"') x = ' ';
  }

  vector<string> args;
  args.push_back(key);
  args.push_back(val); 
  Response r = post(endpoint, REQUEST_HEADERS, compose_invoke(chaincode_name_, "write_multikey", args));
  return add_to_queue(r.body); 
}

string HyperLedgerKVDB::update_multi_key(const string &table, const vector<string> &keys, vector<DB::KVPair> &values, string endpoint) {
  string val = "";
  for (auto v : values) {
    val += v.first + "=" + v.second + " ";
  }
  for (auto &x : val) {
    if (x == '\"') x = ' ';
  }

  vector<string> args;
  for (string k : keys) 
    args.push_back(k);
  args.push_back(val); 
  Response r = post(endpoint, REQUEST_HEADERS, compose_invoke(chaincode_name_, "write_multikey", args));
  return add_to_queue(r.body); 
}


// single key update
int HyperLedgerKVDB::Update(const string &table, const string &key,
                            vector<DB::KVPair> &values) {
  string updateTx = update_single_key(table, key, values, endpoints_[0]); 
  return DB::kOK;
}

string HyperLedgerKVDB::prepare_write(vector<string> &keys, string endpoint) {
  Response r = post(endpoint, REQUEST_HEADERS, compose_invoke(chaincode_name_, "prepare_multiwrite", keys)); 
  return add_to_queue(r.body); 
}

string HyperLedgerKVDB::abort_write(string txid, vector<string> &keys, string endpoint) {
  vector<string> args;
  args.push_back(txid);
  for (string k : keys)
    args.push_back(k); 

  Response r = post(endpoint, REQUEST_HEADERS, compose_invoke(chaincode_name_, "abort_multiwrite", args)); 
  return add_to_queue(r.body); 
}

string HyperLedgerKVDB::commit_write(string txid, vector<string> &keys, vector<DB::KVPair> &values, string endpoint) {
  vector<string> args;
  args.push_back(txid);
  for (string k : keys)
    args.push_back(k); 

  string val = "";
  for (auto v : values) {
    val += v.first + "=" + v.second + " ";
  }
  for (auto &x : val) {
    if (x == '\"') x = ' ';
  }

  args.push_back(val);

  Response r = post(endpoint, REQUEST_HEADERS, compose_invoke(chaincode_name_, "commit_multiwrite", args)); 
  return add_to_queue(r.body); 
}

int HyperLedgerKVDB::UpdateMultiKeys_Prepare(const string &table, const vector<string> &keys,
                            vector<DB::KVPair> &values) {

	 MultiShardTransaction* txn = new MultiShardTransaction(keys, values);

	 // after this, one shard id may have > 1 keys
	 for (string k : keys) {
		int idx = key_to_shard(k);
		if (txn->shardmap.find(idx) == txn->shardmap.end()) {
			txn->shardmap[idx] = vector<string>();
			txn->keyset.push_back(idx);
		}
		txn->shardmap[idx].push_back(k);
	 }

	 txn->start_time = utils::time_now();

	 if (txn->shardmap.size() == 1) { // all the same shard
		 txn->set_state(STATE_SINGLE_SHARD);
		 string txid = update_multi_key(table, keys, values, endpoints_[txn->keyset[0]]);
		 add_to_multishard_queue(txid, txn);
		 return DB::kOK;
	 }

	 txn->set_state(STATE_PREPARE);
	 for (int idx : txn->keyset) {
		 string txid = prepare_write(txn->shardmap[idx], endpoints_[idx]);
		 txn->prepareTxns.push_back(txid);
		 add_to_multishard_queue(txid, txn);
	 }
	 return DB::kOK;
}

void HyperLedgerKVDB::UpdateMultiKeys_Abort(MultiShardTransaction* txn) {
	for (size_t i=0; i<txn->keyset.size(); i++)
		abort_write(txn->prepareTxns[i], txn->shardmap[txn->keyset[i]], endpoints_[txn->keyset[i]]);
}

void HyperLedgerKVDB::UpdateMultiKeys_Commit(MultiShardTransaction* txn) {
	for (size_t i=0; i < txn->keyset.size(); i++) {
		string txid = commit_write(txn->prepareTxns[i], txn->shardmap[txn->keyset[i]], txn->values, endpoints_[txn->keyset[i]]);
	    txn->commitTxns.push_back(txid);
	    add_to_multishard_queue(txid, txn);
	}
}

// ignore table
// ignore field
// concate values in KVPairs into one long value
int HyperLedgerKVDB::Insert(const string &table, const string &key,
                            vector<DB::KVPair> &values) {
  return Update(table, key, values); 
  /*
  string val = "";
  for (auto v : values) {
    val += v.first + "=" + v.second + " ";
  }
  for (auto &x : val) {
    if (x == '\"') x = ' ';
  }
  Response r = post(endpoint_, REQUEST_HEADERS, compose_write(key, val));
  return DB::kOK;
  */
}

// ignore table
// delete value indicated by a key
int HyperLedgerKVDB::Delete(const string &table, const string &key) {
  vector<string> args; 
  args.push_back(key); 
  Response r = post(endpoints_[0], REQUEST_HEADERS, compose_invoke(chaincode_name_, "del", args));
  return DB::kOK;
}

vector<string> HyperLedgerKVDB::find_tx(string key, string json){
  vector<string> ss; 
  size_t key_pos = json.find(key);
  while (key_pos != string::npos){
    auto quote_sign_pos_1 = json.find('\"', key_pos + 1);
    auto quote_sign_pos_2 = json.find('\"', quote_sign_pos_1 + 1);
    auto quote_sign_pos_3 = json.find('\"', quote_sign_pos_2 + 1);
    ss.push_back(json.substr(quote_sign_pos_2 + 1,
                     quote_sign_pos_3 - quote_sign_pos_2 - 1));
    key_pos = json.find(key, quote_sign_pos_3+1);
  }
  return ss; 
}

string HyperLedgerKVDB::get_json_field(const string &json, const string &key) {
  auto key_pos = json.find(key);
  auto quote_sign_pos_1 = json.find('\"', key_pos + 1);
  auto quote_sign_pos_2 = json.find('\"', quote_sign_pos_1 + 1);
  auto quote_sign_pos_3 = json.find('\"', quote_sign_pos_2 + 1);
  return json.substr(quote_sign_pos_2 + 1,
                     quote_sign_pos_3 - quote_sign_pos_2 - 1);
}

bool HyperLedgerKVDB::is_number(const std::string& s) {
    std::string::const_iterator it = s.begin();
    while (it != s.end() && std::isdigit(*it)) ++it;
    return !s.empty() && it == s.end();
}

int HyperLedgerKVDB::find_tip(string json) {
  if (json.find("Failed")!=string::npos) {
    return -1; 
  }
  int key_pos = json.find("height"); 
  auto close_quote_pos = json.find('\"',key_pos+1);   
  auto comma_pos = json.find(',', key_pos+1); 
  string sval = json.substr(close_quote_pos+2, comma_pos-close_quote_pos-2);
  if (is_number(sval))
	  return stoi(sval);
  else
	  std::cout << "find_tip() json: " << json << std::endl;
  return -1;
}

string HyperLedgerKVDB::poll_request(int block_number, string endpoint) {
  string request = endpoint.substr(0,endpoint.find("/chaincode"))+BLOCK_END_POINT+"/"+std::to_string(block_number); 
  return get(request).body; 
}

int HyperLedgerKVDB::get_tip_block_number(string endpoint){
  string request = endpoint.substr(0,endpoint.find("/chaincode"))+CHAIN_END_POINT; 
  return find_tip(get(request).body); 
}

string HyperLedgerKVDB::add_to_queue(string json) {
  return get_json_field(json, "message");
}

void HyperLedgerKVDB::add_to_multishard_queue(string txid, MultiShardTransaction* tx) {
  mstxlock_->lock();
  (*pendingmstx_)[txid] = tx;
  mstxlock_->unlock();
}

/*
bool HyperLedgerKVDB::waitForConfirmation(string txid, double timeout) {
  double start_time = utils::time_now();
  double elapsed; 
  while (true) {
    txlock_->lock();
    if (confirmtx_->find(txid) != confirmtx_->end()) {
      txlock_->unlock();
      return true;
    } else if (failedtx_->find(txid) != failedtx_->end()) {
      txlock_->unlock();
      return false;
    } else {
      txlock_->unlock();
      utils::sleep(0.001); 
    }
    
    elapsed = (utils::time_now() - start_time)/1000000000.0; 
    if (elapsed > timeout) {
      cout << "ABORTING due to TIMEOUT" << endl; 
      return false; 
    }
  }
}
*/


}  // ycsbc

