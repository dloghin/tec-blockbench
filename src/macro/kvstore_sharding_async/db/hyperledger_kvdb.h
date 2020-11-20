#ifndef YCSB_C_HYPERLEDGER_KV_DB_H_
#define YCSB_C_HYPERLEDGER_KV_DB_H_

#include "core/transaction.h"
#include "core/db.h"

#include <iostream>
#include <string>
#include <atomic>
#include <functional>
#include "core/properties.h"
#include "core/transaction.h"
#include <unordered_map>
#include <core/timer.h>
#include <core/utils.h>

using std::cout;
using std::endl;
using std::string;
using std::vector;
using std::unordered_map; 
using std::atomic;

namespace ycsbc {

class HyperLedgerKVDB : public DB {
 public:
  HyperLedgerKVDB(const string path, const string &endpoint, int nshards, int max_nr_keys,
		  int client_index, int clients_per_shard);

  void Init(unordered_map<string, MultiShardTransaction*> *map, SpinLock *lock){
    pendingmstx_ = map;
    mstxlock_ = lock;
  }
  int Read(const string &table, const string &key, const vector<string> *fields,
           vector<KVPair> &result);

  // no scan operation support
  int Scan(const string &table, const string &key, int len,
           const vector<string> *fields, vector<vector<KVPair>> &result) {
    return DB::kOK;
  }

  int Update(const string &table, const string &key, vector<KVPair> &values);

  int Insert(const string &table, const string &key, vector<KVPair> &values);

  int Delete(const string &table, const string &key);

  int find_tip(string json);
  void deploy(const string path, const string &endpoint, int shards, int max_nr_keys, int client_index,
  int clients_per_shard); 

  vector<string> poll_tx(int block_number); 
  int get_tip_block_number(string endpoint); 
  string add_to_queue(string json); // add to pending queue
  string poll_request(int block_number, string endpoint); // polling from an endpoint
  bool waitForConfirmation(string txid, double timeout); // blocking
  string update_single_key(const string &table, const string &key, vector<KVPair> &values, string endpoint);
  string update_multi_key(const string &table, const vector<string> &keys, vector<KVPair> &values, string endpoint);

  string prepare_write(vector<string> &keys, string endpoint); 
  string abort_write(string txid, vector<string> &keys, string endpoint); 
  string commit_write(string txid, vector<string> &keys, vector<KVPair> &value, string endpoint); 
  vector<string> find_tx(string key, string json); // key can be txid or TxID 

  void add_to_multishard_queue(string txid, MultiShardTransaction* tx);
  int UpdateMultiKeys_Prepare(const string &table, const vector<string> &keys, vector<DB::KVPair> &values);
  void UpdateMultiKeys_Abort(MultiShardTransaction* txn);
  void UpdateMultiKeys_Commit(MultiShardTransaction* txn);

 private:
  unordered_map<string, MultiShardTransaction*> *pendingmstx_;
  SpinLock *mstxlock_;

  string get_json_field(const string &json, const string &key); 
    atomic<long> post_count_;

  bool is_number(const std::string& s);

  vector<string> endpoints_;
  int nshards_, max_nr_keys_;
  string chaincode_name_; 

  int key_to_shard(string key) {
    std::hash<string> hasher; 
    return hasher(key) % nshards_; 
  }
};

}  // ycsbc

#endif  // YCSB_C_HYPERLEDGER_KV_DB_H_
