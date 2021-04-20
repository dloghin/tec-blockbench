#ifndef BLOCKBENCH_CORDA_DB_H_
#define BLOCKBENCH_CORDA_DB_H_

#include <iostream>
#include <string>
#include <unordered_map>

#include "core/properties.h"
#include "core/timer.h"
#include "core/utils.h"
#include "core/bb_utils.h"
#include "core/db.h"

namespace ycsbc {

#define API_ENDPOINT  "/api/kvstore/kvstore"

const std::string REQUEST_HEADERS = "application/json";

enum Command { None, Write, Delete, Update, Read };


class CordaDB : public DB {
 public:
	CordaDB(const std::string &endpoint, const std::string &wl_name);

  void Init(std::unordered_map<std::string, double> *pendingtx,
            SpinLock *lock) {
    pendingtx_ = pendingtx;
    txlock_ = lock;
  }
  int Read(const std::string &table, const std::string &key,
           const std::vector<std::string> *fields, std::vector<KVPair> &result);

  // no scan operation support
  int Scan(const std::string &table, const std::string &key, int len,
           const std::vector<std::string> *fields,
           std::vector<std::vector<KVPair>> &result) {
    return DB::kOK;
  }

  int Update(const std::string &table, const std::string &key,
             std::vector<KVPair> &values);

  int Insert(const std::string &table, const std::string &key,
             std::vector<KVPair> &values);

  int Delete(const std::string &table, const std::string &key);

  int GetTip();

  std::vector<std::string> PollTxn(int block_number);

 private:
  std::string endpoint_;
  std::unordered_map<std::string, double> *pendingtx_;
  SpinLock *txlock_;

  BBUtils::SmartContractType sctype_;
};

}  // ycsbc

#endif  // BLOCKBENCH_CORDA_DB_H_
