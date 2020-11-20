//
//  ycsbc.cc
//  YCSB-C
//
//  Created by Jinglei Ren on 12/19/14.
//  Copyright (c) 2014 Jinglei Ren <jinglei@ren.systems>.
//

#include <cstring>
#include <string>
#include <iostream>
#include <vector>
#include <queue>
#include <future>
#include <atomic>
#include <sstream>
#include <sched.h>
#include <sys/signal.h>
#include "core/utils.h"
#include "core/timer.h"
#include "core/client.h"
#include "core/core_workload.h"
#include "core/statistic.h"
#include "db/db_factory.h"
#include "db/ethereum_kvdb.h"
#include "db/hyperledger_kvdb.h"
#include "db/parity_kvdb.h"
#include "core/db.h"
#include "db/db_utils.h"
#include "core/transaction.h"

using namespace std;
using ycsbc::EthereumKVDB;
using ycsbc::HyperLedgerKVDB;
using ycsbc::ParityKVDB;
const unsigned int BLOCK_POLLING_INTERVAL = 2;
const unsigned int CONFIRM_BLOCK_LENGTH = 5;
const unsigned int HL_CONFIRM_BLOCK_LENGTH = 1;
const unsigned int PARITY_CONFIRM_BLOCK_LENGTH = 1;

int txrate = 10; 

volatile bool runClientsFlag = true;
volatile bool runStatusFlag = true;

std::atomic<unsigned long long> latency(0);
std::atomic<unsigned long long> latency_interval(0);
std::atomic<unsigned long long> ops(0);
std::atomic<int> nsuccesses(0), nfails(0), nsingleshard(0), nmultishard(0);

// std::unordered_map<string, double> pendingtx, confirmtx, failedtx;

std::queue<MultiShardTransaction*> toDelete, toAbort;
std::unordered_map<string, MultiShardTransaction*> pendingMultiShardTx;

void UsageMessage(const char *command);
bool StrStartWith(const char *str, const char *pre);
string ParseCommandLine(int argc, const char *argv[], utils::Properties &props);

// locking the pendingtx queue
// SpinLock spinlock_, txlock_, mstxlock_, toDeleteLock_, toAbortLock_;
SpinLock mstxlock_, toDeleteLock_, toAbortLock_;

void signal_handler_sigint(int status) {
	runStatusFlag = false;
}

void signal_handler_sigusr1(int status) {
	runClientsFlag = false;
}

int ShardClientThread(HyperLedgerKVDB *db, ycsbc::CoreWorkload *wl, const int num_ops, int opt) {
  std::cout << "Client Thread running on core " << sched_getcpu() << std::endl;

  double tx_sleep_time = 1.0 / txrate;
  double start_time = utils::time_now();
  double st = start_time;
  double current_time = 0;
  double elapsed_time = 0;
  for (int i=0; i<num_ops; i++) {
      if (opt==1) { // single key update
        //string key = wl->NextSequenceKey();
        string key = wl->NextTransactionKey(); 
        vector<ycsbc::DB::KVPair> pairs;
        wl->BuildValues(pairs); 
        db->Update(wl->NextTable(), key, pairs);
      } else {
        vector<string> keys;
        for (int j=0; j<2; j++) 
          //keys.push_back(wl->NextSequenceKey()); 
          keys.push_back(wl->NextTransactionKey());
        vector<ycsbc::DB::KVPair> pairs; 
        wl->BuildValues(pairs);
        db->UpdateMultiKeys_Prepare(wl->NextTable(), keys, pairs);
      }
      current_time = utils::time_now(); 
      elapsed_time = (current_time - st)/1000000000.0; 
      if (elapsed_time < tx_sleep_time)
        utils::sleep(tx_sleep_time - elapsed_time); 
      st = current_time;
      if (!runClientsFlag || !runStatusFlag) {
    	  std::cout << "Stopping client thread..." << std::endl;
    	  break;
      }
  }
  return nsuccesses; 
}

int CleaningThread(HyperLedgerKVDB *db) {
	cout << "Cleaning Thread running on core " << sched_getcpu() << std::endl;

	vector<MultiShardTransaction*> to_abort;
	queue<MultiShardTransaction*> to_delete;
	while (runStatusFlag) {
		toAbortLock_.lock();
		while (!toAbort.empty()) {
			to_abort.push_back(toAbort.front());
			toAbort.pop();
		}
		toAbortLock_.unlock();
		toDeleteLock_.lock();
		while (!toDelete.empty()) {
			to_delete.push(toDelete.front());
			toDelete.pop();
		}
		toDeleteLock_.unlock();
		for (vector<MultiShardTransaction*>::iterator it = to_abort.begin(); it != to_abort.end(); it++) {
			db->UpdateMultiKeys_Abort(*it);
		}
		to_abort.clear();
		while (!to_delete.empty()) {
			MultiShardTransaction* txn = to_delete.front();
			to_delete.pop();
			delete txn;
		}
		utils::sleep(0.5);
	}
	std::cout << "Stopping cleaning thread..." << std::endl;
	return 0;
}

int HL_StatusThread(HyperLedgerKVDB *db, double interval, string endpoint) {
  cout << "Status Thread running on core " << sched_getcpu() << std::endl;

  vector<string> endpoints = split(endpoint, ';'); 
  vector<int> start_block_height; 
  for (size_t i=0; i<endpoints.size(); i++)
    start_block_height.push_back(db->get_tip_block_number(endpoints[i])); 

  long start_time;
  long end_time;
  int txcount = 0;
  long latency;
  int confirm_duration = HL_CONFIRM_BLOCK_LENGTH;
  int nshards = endpoints.size();

  vector<MultiShardTransaction*> to_abort;
  vector<MultiShardTransaction*> to_delete;

  // loop through all endpoints to poll
  while (runStatusFlag) {
    start_time = utils::time_now();
    for (int i=0; i<nshards; i++) {
      int tip = db->get_tip_block_number(endpoints[i]); 
      if (tip==-1) {
        cout << "Fail to get tips from Hyperledger" << endl;
        continue;
      }

      while (start_block_height[i] + confirm_duration <= tip) {      
        string poll_res = db->poll_request(start_block_height[i], endpoints[i]); 
        vector<string> txs = db->find_tx("txid", poll_res); // successful txs
        vector<string> failed_txs = db->find_tx("txID", poll_res);
        cout << "polled block from " << endpoints[i] << ", height:  " << start_block_height[i] << " size : " << txs.size() 
           << " txs " << ", failed txs: " << failed_txs.size() << endl; 
        start_block_height[i]++;
        long block_time = utils::time_now();
        // update states and timings
        mstxlock_.lock();
        for (string tmp : txs) {
        	if (pendingMultiShardTx.find(tmp) != pendingMultiShardTx.end()) {
        		MultiShardTransaction* txn = pendingMultiShardTx.find(tmp)->second;
        		int state = txn->update_state(true, tmp);
        		switch (state) {
        		case STATE_SINGLE_SHARD:
        			nsuccesses++;
        			txcount++;
        			latency += (block_time - txn->start_time);
        			to_delete.push_back(txn);
        			nsingleshard++;
        			break;
        		case STATE_ABORT:
        			nfails++;
        			txcount++;
        			nmultishard++;
        			to_abort.push_back(txn);
        			break;
        		case STATE_DONE_SUCCESS:
        			nsuccesses++;
        			txcount++;
        			nmultishard++;
        			latency += (block_time - txn->start_time);
        			to_delete.push_back(txn);
        			break;
        		case STATE_DONE_FAIL:
        			nfails++;
        			txcount++;
        			nmultishard++;
        			to_delete.push_back(txn);
        			break;
        		}
        	}
        }
        for (string tmp : failed_txs) {
        	if (pendingMultiShardTx.find(tmp) != pendingMultiShardTx.end()) {
        		MultiShardTransaction* txn = pendingMultiShardTx.find(tmp)->second;
        		int state = txn->update_state(false, tmp);
        		switch (state) {
        		case STATE_SINGLE_SHARD:
        			nfails++;
        			txcount++;
        			nsingleshard++;
        			to_delete.push_back(txn);
        			break;
        		case STATE_ABORT:
        			nfails++;
        			txcount++;
        			nmultishard++;
        			to_abort.push_back(txn);
        			break;
        		case STATE_DONE_SUCCESS:	// should not get here
        			nsuccesses++;
        			txcount++;
        			nmultishard++;
        			to_delete.push_back(txn);
        			break;
        		case STATE_DONE_FAIL:
        			nfails++;
        			txcount++;
        			nmultishard++;
        			to_delete.push_back(txn);
        			break;
        		}
        	}
        }
        mstxlock_.unlock();
      }
    }

    toAbortLock_.lock();
    for (vector<MultiShardTransaction*>::iterator it = to_abort.begin(); it != to_abort.end(); it++)
    	toAbort.push(*it);
    toAbortLock_.unlock();
    to_abort.clear();
    toDeleteLock_.lock();
    for (vector<MultiShardTransaction*>::iterator it = to_delete.begin(); it != to_delete.end(); it++)
    	toDelete.push(*it);
    toDeleteLock_.unlock();
    to_delete.clear();

    cout << "In the last "<< interval << "s, tx count = " << txcount
        << " latency = " << latency/1000000000.0
        << " outstanding request = " << pendingMultiShardTx.size()
		<< ", nsuccesses " << nsuccesses << ", nfails " << nfails
		<< ", nSingleShard " << nsingleshard << ", nMultiShard " << nmultishard << endl;

    txcount = 0; 
    latency = 0; 

    end_time = utils::time_now(); 

    //sleep in nanosecond
    double delay = interval - (end_time - start_time) / 1000000000.0;
    if (delay > 0)
      utils::sleep(delay); 
  }
  std::cout << "Stopping status thread..." << std::endl;
  return 0;
}


int main(const int argc, const char *argv[]) {
  signal(SIGINT, signal_handler_sigint);
  signal(SIGUSR1, signal_handler_sigusr1);

  utils::Properties props;
  string file_name = ParseCommandLine(argc, argv, props);

  /*
  ycsbc::DB *db = ycsbc::DBFactory::CreateDB(props);
  if (!db) {
    cout << "Unknown database name " << props["dbname"] << endl;
    exit(0);
  }
  */
  HyperLedgerKVDB *dbhandler_hl;
  int current_tip = 0;
  dbhandler_hl = new HyperLedgerKVDB(props["chainID"], props["endpoint"],
                      stoi(props["nshards"]), stoi(props["recordcount"]),
                      stoi(props["client_index"]), stoi(props["clients_per_shard"])); 

  dbhandler_hl->Init(&pendingMultiShardTx, &mstxlock_);

  cout << "The Current TIP = " << current_tip << endl;
  ycsbc::CoreWorkload wl;
  wl.Init(props);

  const int num_threads = stoi(props.GetProperty("threadcount", "1"));

  // Loads data
  vector<future<int>> actual_ops;
  //int total_ops = stoi(props[ycsbc::CoreWorkload::RECORD_COUNT_PROPERTY]);
  int total_ops = stoi(props["recordcount"]); 
  for (int i = 0; i < num_threads; ++i) {
    actual_ops.emplace_back(async(launch::async, ShardClientThread, dbhandler_hl, &wl,
          total_ops / num_threads, stoi(props["operation"])));
  }

  actual_ops.emplace_back(async(launch::async, HL_StatusThread, dbhandler_hl,
                          BLOCK_POLLING_INTERVAL, props["endpoint"]));

  actual_ops.emplace_back(async(launch::async, CleaningThread, dbhandler_hl));

  int sum = 0;
  for (auto &n : actual_ops) {
    assert(n.valid());
    sum += n.get();
  }

  std::cout << "Done." << std::endl;
}

string ParseCommandLine(int argc, const char *argv[],
                        utils::Properties &props) {
  int argindex = 1;
  string filename;
  while (argindex < argc && StrStartWith(argv[argindex], "-")) {
    if (strcmp(argv[argindex], "-threads") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("threadcount", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-ops") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("ops_interval", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-operation") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("operation", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-stat") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("stat_output", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-db") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("dbname", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-host") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("host", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-recordcount") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("recordcount", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-port") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("port", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-slaves") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("slaves", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-retry") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("retry", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-retry_time_interval") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("time_interval", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-minimum_depth") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("minimum_depth", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-endpoint") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("endpoint", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-chainID") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("chainID", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-nshards") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("nshards", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-client_index") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("client_index", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-clients_per_shard") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("clients_per_shard", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-pre_query") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("pre_query", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-requestdistribution") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      props.SetProperty("requestdistribution", argv[argindex]);
      argindex++;
    } 
    else if (strcmp(argv[argindex], "-txrate")==0){
      argindex++;
      txrate = atoi(argv[argindex]);
      argindex++; 
    }
    else if (strcmp(argv[argindex], "-P") == 0) {
      argindex++;
      if (argindex >= argc) {
        UsageMessage(argv[0]);
        exit(0);
      }
      filename.assign(argv[argindex]);
      ifstream input(argv[argindex]);
      try {
        props.Load(input);
      } catch (const string &message) {
        cout << message << endl;
        exit(0);
      }
      input.close();
      argindex++;
    } else if (strcmp(argv[argindex], "-txrate")==0){
      argindex++; 
      txrate = atoi(argv[argindex]);
      argindex++; 
    } else if (strcmp(argv[argindex], "-fieldcount")==0) {
      argindex++;
      props.SetProperty("fieldcount", argv[argindex]);
      argindex++;
    } else if (strcmp(argv[argindex], "-zipfian")==0) {
      argindex++;
      props.SetProperty("zipfian", argv[argindex]);
      argindex++;
    }

    else {
      cout << "Unknown option '" << argv[argindex] << "'" << endl;
      exit(0);
    }
  }

  if (argindex == 1 || argindex != argc) {
    UsageMessage(argv[0]);
    exit(0);
  }

  return filename;
}

void UsageMessage(const char *command) {
  cout << "Usage: " << command << " [options]" << endl;
  cout << "Options:" << endl;
  cout << "  -threads n: execute using n threads (default: 1)" << endl;
  cout << "  -ops ops_interval: per ops_interval operations we will do a "
          "statistic output "
          "to file" << endl;
  cout << "  -stat statistic_output_file: per ops_interval operations we will "
          "do a statistic output "
          "to THIS file" << endl;
  cout << "  -db dbname: specify the name of the DB to use (default: basic)"
       << endl;
  cout << "  -P propertyfile: load properties from the given file. Multiple "
          "files can" << endl;
  cout << "                   be specified, and will be processed in the order "
          "specified" << endl;
}

inline bool StrStartWith(const char *str, const char *pre) {
  return strncmp(str, pre, strlen(pre)) == 0;
}
