//
//  basic_db.cc
//  YCSB-C
//
//  Created by Jinglei Ren on 12/17/14.
//  Copyright (c) 2014 Jinglei Ren <jinglei@ren.systems>.
//

#include "db/db_factory.h"

#include <string>
#include "db/basic_db.h"
#include "db/lock_stl_db.h"
//#include "db/tbb_rand_db.h"
//#include "db/tbb_scan_db.h"
#include "db/hyperledger_kvdb.h"
#include "db/ethereum_kvdb.h"
#include "db/parity_kvdb.h"

using namespace std;
using ycsbc::DB;
using ycsbc::DBFactory;

DB* DBFactory::CreateDB(utils::Properties& props) {
  if (props["dbname"] == "basic") {
    return new BasicDB;
  } else if (props["dbname"] == "lock_stl") {
    return new LockStlDB;
//  } else if (props["dbname"] == "tbb_rand") {
//    return new TbbRandDB;
//  } else if (props["dbname"] == "tbb_scan") {
//    return new TbbScanDB;
  } else if (props["dbname"] == "hyperledger") {
    const string endpoint = props["endpoint"];
    return NULL; //new HyperLedgerKVDB(endpoint);
  } else if (props["dbname"] == "ethereum") {
    //unsigned retry = stoi(props["retry"]);
    //unsigned thread_retry_time_interval = stoi(props["time_interval"]);
    //unsigned minimum_depth = stoi(props["minimum_depth"]);
    unsigned retry = 0; 
    unsigned thread_retry_time_interval = 0; 
    unsigned minimum_depth = 0; 
    const string endpoint = props["endpoint"];
    return new EthereumKVDB(endpoint, retry, thread_retry_time_interval,
                            minimum_depth);
  } else if (props["dbname"] == "parity") {
    unsigned retry = 0; 
    unsigned thread_retry_time_interval = 0; 
    unsigned minimum_depth = 0; 
    const string endpoint = props["endpoint"];
    return new ParityKVDB(endpoint, retry, thread_retry_time_interval,
                            minimum_depth);
  } else
    return NULL;
}
