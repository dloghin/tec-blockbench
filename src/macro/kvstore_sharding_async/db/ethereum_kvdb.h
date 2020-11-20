#ifndef YCSB_C_ETHEREUM_KV_DB_H_
#define YCSB_C_ETHEREUM_KV_DB_H_

#include "core/db.h"

#include <iostream>
#include <string>
#include "core/properties.h"
#include <core/utils.h>
#include <core/timer.h>
#include <unordered_map>

using std::cout;
using std::endl;
using std::string;
using std::vector;
using std::unordered_map; 

namespace ycsbc {

class EthereumKVDB : public DB {
 public:
  EthereumKVDB(const string &endpoint, unsigned retry,
               unsigned thread_retry_time_interval, unsigned minimum_depth);

  void Init(unordered_map<string, double> *pendingtx, SpinLock *lock){
    pendingtx_ = pendingtx; 
    txlock_ = lock; 
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

  unsigned int get_tip_block_number();
  vector<string> poll_tx(int block_number); 
  vector<string> poll_tx_by_hash(string block_hash); 

 private:
  unordered_map<string, double> *pendingtx_; 
  SpinLock *txlock_; 

  string from_address_, to_address_, endpoint_;
  unsigned deploy_wait_time_, retry_, thread_retry_time_interval_,
      minimum_depth_;
  string get_from_address();
  int get_txn_block_number(const string &txn_hash);
  string get_json_field(const string &json, const string &key);
  vector<string> get_list_field(const string &json, const string &key); 
  string compose_get_transaction(const string &txn_hash);
  string compose_del(const string &key);
  string compose_write(const string &key, const string &val);
  string compose_read(const string &key);
  string encode_get(const string &key);
  string encode_set(const string &key, const string &value);
  string encode_string(const string &str);
  string right_padding_string(const string &str);
  string left_padding_string(const string &str);
  string utf8_hex_encode(const string &str);
  unsigned int parse_hex(const string &s);
  string hex(unsigned int c);
};

}  // ycsbc

#endif  // YCSB_C_ETHEREUM_KV_DB_H_
