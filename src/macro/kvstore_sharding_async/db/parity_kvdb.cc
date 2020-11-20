#include "db/parity_kvdb.h"
#include "db/db_utils.h"
#include <restclient-cpp/restclient.h>
#include <string>
#include <vector>
#include <chrono>
#include <thread>
#include <sstream>
#include <cassert>
#include <iostream>
//#include <boost/algorithm/string.hpp>

using namespace RestClient;
using namespace std;

namespace ycsbc {

const unsigned int SMART_CONTRACT_DEPLOY_WAIT_TIME = 30;
const string REQUEST_HEADERS = "application/json";
//const string REQUEST_HEADERS="/post"; 

const string UNLOCK_ACCOUNT_PREFIX =
    "{\
  \"jsonrpc\": \"2.0\", \
  \"method\": \"personal_unlockAccount\", \
  \"params\": [\"";

const string UNLOCK_ACCOUNT_SUFFIX =
    "\",\"\",null], \
      \"id\": 1}";

const string SEND_TXN_PREFIX =
    "{\
  \"jsonrpc\": \"2.0\", \
  \"method\": \"eth_sendTransaction\", \
  \"params\": [{ \"gas\": \"0x10000000\", \
                \"gasPrice\": \"0x0\", \
                \"from\": \"";

const string CALL_PREFIX =
    "{\
  \"jsonrpc\": \"2.0\", \
  \"method\": \"eth_call\", \
  \"params\": [{ \"gas\": \"0x10000000\",\
                \"gasPrice\": \"0x0\", \
                \"from\": \"";
const string GET_TXN_PREFIX =
    "{\
  \"jsonrpc\": \"2.0\", \
  \"method\": \"eth_getTransactionByHash\", \
  \"params\": [\"";

const string MIDDLE_PART_1 = "\", \"to\": \"";
const string MIDDLE_PART_2 = "\", \"data\": \"";
const string SEND_TXN_SUFFIX = "\"}],\"id\":1}";
const string CALL_SUFFIX = "\"},\"latest\"],\"id\":1}";
const string GET_TXN_SUFFIX = "\"],\"id\":1}";

const string GET_BLOCK_BY_NUMBER_PREFIX =
    "{\"jsonrpc\":\"2.0\", \
      \"method\":\"eth_getBlockByNumber\", \
      \"params\":[\"";
const string GET_BLOCK_BY_NUMBER_SUFFIX =
    "\", false], \
      \"id\": 1}";

const string GET_BLOCK_BY_HASH_PREFIX =
    "{\"jsonrpc\":\"2.0\", \
      \"method\":\"eth_getBlockByHash\", \
      \"params\":[\"";
const string GET_BLOCK_BY_HASH_SUFFIX =
    "\", false], \
      \"id\": 1}";

const string GET_BLOCKNUMBER =
    "{\"jsonrpc\":\"2.0\", \
      \"method\":\"eth_blockNumber\", \
      \"params\":[], \
      \"id\": 1}";
const string GET_ACCOUNTS =
    "{\"jsonrpc\":\"2.0\",\"method\":\"eth_accounts\",\"params\":[],\"id\":1}";

const string DEPLOY_KV_SMARTCONTRACT_PREFIX =
    " {\"jsonrpc\":\"2.0\",\
  \"method\":\"eth_sendTransaction\",\
  \"params\": [{ \"gas\": \"0x1000000\",\
                \"gasPrice\": \"0x0\", \
               \"from\": \"";

const string DEPLOY_KV_SMARTCONTRACT_SUFFIX =
    "\", \"data\": "
    "\"0x6060604052610398806100126000396000f360606040526000357c0100000000000000"
    "00"
    "000000000000000000000000000000000000000090048063693ec85e14610047578063e942"
    "b5161461010e57610042565b610002565b34610002576100a0600480803590602001908201"
    "8035906020019191908080601f016020809104026020016040519081016040528093929190"
    "8181526020018383808284378201915050505050509090919050506101b0565b6040518080"
    "60200182810382528381815181526020019150805190602001908083838290600060046020"
    "84601f0104600302600f01f150905090810190601f16801561010057808203805160018360"
    "20036101000a031916815260200191505b509250505060405180910390f35b346100025761"
    "01ae6004808035906020019082018035906020019191908080601f01602080910402602001"
    "60405190810160405280939291908181526020018383808284378201915050505050509090"
    "91908035906020019082018035906020019191908080601f01602080910402602001604051"
    "90810160405280939291908181526020018383808284378201915050505050509090919050"
    "506102aa565b005b6020604051908101604052806000815260200150600060005082604051"
    "808280519060200190808383829060006004602084601f0104600302600f01f15090500191"
    "50509081526020016040518091039020600050805460018160011615610100020316600290"
    "0480601f016020809104026020016040519081016040528092919081815260200182805460"
    "0181600116156101000203166002900480156102995780601f1061026e5761010080835404"
    "0283529160200191610299565b820191906000526020600020905b81548152906001019060"
    "200180831161027c57829003601f168201915b505050505090506102a5565b919050565b80"
    "600060005083604051808280519060200190808383829060006004602084601f0104600302"
    "600f01f1509050019150509081526020016040518091039020600050908051906020019082"
    "8054600181600116156101000203166002900490600052602060002090601f016020900481"
    "019282601f1061033557805160ff1916838001178555610366565b82800160010185558215"
    "610366579182015b8281111561036557825182600050559160200191906001019061034756"
    "5b5b5090506103919190610373565b8082111561038d576000818150600090555060010161"
    "0373565b5090565b50505b505056\"}],\"id\":1}";

const string GET_SMART_CONTRACT_ADDRESS_PREFIX =
    "{\"jsonrpc\":\"2.0\",\
       \"method\":\"eth_getTransactionReceipt\",\
       \"params\":[\"";

const string GET_SMART_CONTRACT_ADDRESS_SUFFIX = "\"],\"id\":1}";

string ParityKVDB::hex(unsigned int c) {
  std::ostringstream stm;
  stm << std::hex << c;
  return stm.str();
}

unsigned int ParityKVDB::parse_hex(const string &s) {
  unsigned int ret;
  std::stringstream stm;
  stm << std::hex << s;
  stm >> ret;
  return ret;
}

string ParityKVDB::utf8_hex_encode(const string &str) {
  string result;
  for (unsigned char c : str) result += hex(c);
  return result;
}

string ParityKVDB::left_padding_string(const string &str) {
  if (str.length() < 64) {
    string ret = str;
    while (ret.length() != 64) ret = "0" + ret;
    return ret;
  } else {
    return str;
  }
}

string ParityKVDB::right_padding_string(const string &str) {
  string ret = str;
  for (unsigned i = 0; i < 64 - (str.length() % 64); ++i) ret += '0';
  return ret;
}

string ParityKVDB::encode_string(const string &str) {
  string utf8_encoded = utf8_hex_encode(str);

  string l = hex(utf8_encoded.length() / 2);
  string ret = left_padding_string(l) + right_padding_string(utf8_encoded);
  return ret;
}

string ParityKVDB::encode_set(const string &key, const string &value) {
  string ret =
      "0xe942b51600000000000000000000000000000000000000000000000000000000000000"
      "40";
  string argument_1 = encode_string(key);
  ret += left_padding_string(hex(argument_1.length()));
  ret += argument_1 + encode_string(value);
  return ret;
}

string ParityKVDB::encode_get(const string &key) {
  string ret =
      "0x693ec85e00000000000000000000000000000000000000000000000000000000000000"
      "20";
  return ret + encode_string(key);
}

string ParityKVDB::compose_read(const string &key) {
  return CALL_PREFIX + from_address_ + MIDDLE_PART_1 + to_address_ +
         MIDDLE_PART_2 + encode_get(key) + CALL_SUFFIX;
}

string ParityKVDB::compose_write(const string &key, const string &val) {
  return SEND_TXN_PREFIX + from_address_ + MIDDLE_PART_1 + to_address_ +
         MIDDLE_PART_2 + encode_set(key, val) + SEND_TXN_SUFFIX;
}

string ParityKVDB::compose_del(const string &key) {
  return compose_write(key, "NULL");
}

string ParityKVDB::compose_get_transaction(const string &txn_hash) {
  return GET_TXN_PREFIX + txn_hash + GET_TXN_SUFFIX;
}

string ParityKVDB::get_json_field(const string &json, const string &key) {
  auto key_pos = json.find(key);
  auto quote_sign_pos_1 = json.find('\"', key_pos + 1);
  auto quote_sign_pos_2 = json.find('\"', quote_sign_pos_1 + 1);
  auto quote_sign_pos_3 = json.find('\"', quote_sign_pos_2 + 1);
  return json.substr(quote_sign_pos_2 + 1,
                     quote_sign_pos_3 - quote_sign_pos_2 - 1);
}

unsigned int ParityKVDB::get_tip_block_number() {
  Response r = post(endpoint_, REQUEST_HEADERS, GET_BLOCKNUMBER);
  return parse_hex(get_json_field(r.body, "result"));
}

int ParityKVDB::get_txn_block_number(const string &txn_hash) {
  Response r =
      post(endpoint_, REQUEST_HEADERS, compose_get_transaction(txn_hash));
  // in case this transaction haven't been mined, return MAXIMUM uint.
  if (r.body.find("\"blockNumber\":null") != string::npos) return -1;
  return parse_hex(get_json_field(r.body, "blockNumber"));
}

string ParityKVDB::get_from_address() {
  cout << GET_ACCOUNTS << endl; 
  auto r = post(endpoint_, REQUEST_HEADERS, GET_ACCOUNTS).body;
  return get_json_field(r, "result");
}

vector<string> ParityKVDB::get_list_field(const string &json,
                                          const string &key) {
  auto key_pos = json.find(key);
  auto quote_sign_pos_1 = json.find('\"', key_pos + 1);
  auto quote_sign_pos_2 = json.find('[', quote_sign_pos_1 + 1);
  auto quote_sign_pos_3 = json.find(']', quote_sign_pos_2 + 1);

  return split(json.substr(quote_sign_pos_2 + 1,
                           quote_sign_pos_3 - quote_sign_pos_2 - 1),
               ',');
}

vector<string> ParityKVDB::poll_tx_by_hash(string block_hash) {
  string request =
      GET_BLOCK_BY_HASH_PREFIX + block_hash + GET_BLOCK_BY_HASH_SUFFIX;
  auto r = post(endpoint_, REQUEST_HEADERS, request).body;
  vector<string> ss = get_list_field(r, "transactions");
  return ss;
}

// get all tx from the start_block until latest
vector<string> ParityKVDB::poll_tx(int block_number) {
  string request = GET_BLOCK_BY_NUMBER_PREFIX + to_string(block_number) +
                   GET_BLOCK_BY_NUMBER_SUFFIX;

  auto r = post(endpoint_, REQUEST_HEADERS, request).body;
  cout << "block " << block_number << " : " << get_json_field(r,"hash") << endl; 
  vector<string> ss = get_list_field(r, "transactions");
  vector<string> uncles = get_list_field(r, "uncles");
  for (string uncle : uncles) {
    vector<string> uncletxs = poll_tx_by_hash(uncle);
    for (string tx : uncletxs) ss.push_back(tx);
  }
  return ss;
}

ParityKVDB::ParityKVDB(const string &endpoint, unsigned retry,
                       unsigned thread_retry_time_interval,
                       unsigned minimum_depth)
    : endpoint_(endpoint),
      retry_(retry),
      thread_retry_time_interval_(thread_retry_time_interval),
      minimum_depth_(minimum_depth) {
  conn_ = new RestClient::Connection(endpoint); 
  RestClient::HeaderFields headers; 
  headers["Expect"]="";
  conn_->SetHeaders(headers);

  cout << " Sending to .. " << endpoint_ << endl; 
  
  from_address_ = get_from_address();
  cout << "From ... " << from_address_ << endl; 
  
  Response r0 =
      post(endpoint_, REQUEST_HEADERS,
           UNLOCK_ACCOUNT_PREFIX + from_address_ + UNLOCK_ACCOUNT_SUFFIX);
    
  Response r1 = post(endpoint_, REQUEST_HEADERS,
                     DEPLOY_KV_SMARTCONTRACT_PREFIX + from_address_ +
                         DEPLOY_KV_SMARTCONTRACT_SUFFIX);

  string recepit = get_json_field(r1.body, "result");
  std::this_thread::sleep_for(
      std::chrono::seconds(SMART_CONTRACT_DEPLOY_WAIT_TIME));
  Response r2 = post(endpoint_, REQUEST_HEADERS,
                     GET_SMART_CONTRACT_ADDRESS_PREFIX + recepit +
                         GET_SMART_CONTRACT_ADDRESS_SUFFIX);

  cout << GET_SMART_CONTRACT_ADDRESS_PREFIX + recepit +
   GET_SMART_CONTRACT_ADDRESS_SUFFIX << endl;
   cout << r2.body << endl;

  assert(r2.body.find("\"result\":null") == string::npos);
  to_address_ = get_json_field(r2.body, "contractAddress");
  cout << "to address: " << to_address_ << endl;
  cout << "Smart contract deploy ready" << endl;
}

// ignore table
// ignore field
// read value indicated by a key
int ParityKVDB::Read(const string &table, const string &key,
                     const vector<string> *fields, vector<KVPair> &result) {
  Response r0 =
      post(endpoint_, REQUEST_HEADERS,
           UNLOCK_ACCOUNT_PREFIX + from_address_ + UNLOCK_ACCOUNT_SUFFIX);
  Response r = post(endpoint_, REQUEST_HEADERS, compose_read(key));
  txlock_->lock();
  (*pendingtx_)[get_json_field(r.body, "result")] = utils::time_now();
  txlock_->unlock();
  return DB::kOK;
}

// ignore table
// update value indicated by a key
int ParityKVDB::Update(const string &table, const string &key,
                       vector<KVPair> &values) {
  string val = "";
  for (auto v : values) {
    val += v.first + "=" + v.second + " ";
    // val = "hello";
  }
  Response r0 =
      post(endpoint_, REQUEST_HEADERS,
           UNLOCK_ACCOUNT_PREFIX + from_address_ + UNLOCK_ACCOUNT_SUFFIX);

  double start_time = utils::time_now();
  Response r = post(endpoint_, REQUEST_HEADERS, compose_write(key, val));
  string txn_hash = get_json_field(r.body, "result");
  // double end_time = utils::time_now();

  txlock_->lock();
  (*pendingtx_)[txn_hash] = start_time;
  // cout << "added tx " << txn_hash << " time = " << (*pendingtx_)[txn_hash] <<
  // endl;
  txlock_->unlock();

  /*
  unsigned txn_height = get_txn_block_number(txn_hash);
  unsigned tip = get_tip_block_number();
  bool flag = false;
  if (txn_height != -1 && tip - txn_height >= minimum_depth_) flag = true;
  // cout << "txn: " << txn_height << endl;
  unsigned retry = retry_;
  while (!flag && retry) {
    unsigned txn_height = get_txn_block_number(txn_hash);
    unsigned tip = get_tip_block_number();
    std::this_thread::sleep_for(
        std::chrono::milliseconds(thread_retry_time_interval_));
    if (txn_height != -1 && tip - txn_height >= minimum_depth_) flag = true;
    --retry;
  }
  */
  // cout << tip << " : " << txn_height << endl;

  return DB::kOK;
}

// ignore table
// ignore field
// concate values in KVPairs into one long value
int ParityKVDB::Insert(const string &table, const string &key,
                       vector<KVPair> &values) {
  return Update(table, key, values);
}

// ignore table
// delete value indicated by a key
int ParityKVDB::Delete(const string &table, const string &key) {
  Response r = post(endpoint_, REQUEST_HEADERS, compose_del(key));

  string txn_hash = get_json_field(r.body, "result");
  int txn_height = get_txn_block_number(txn_hash);
  unsigned tip = get_tip_block_number();
  bool flag = false;
  if (txn_height != -1 && tip - txn_height >= minimum_depth_) flag = true;
  unsigned retry = retry_;
  while (!flag && retry) {
    txn_height = get_txn_block_number(txn_hash);
    tip = get_tip_block_number();
    std::this_thread::sleep_for(
        std::chrono::milliseconds(thread_retry_time_interval_));
    if (txn_height != -1 && tip - txn_height >= minimum_depth_) flag = true;
    --retry;
  }
  if (flag)
    return DB::kOK;
  else
    return DB::kErrorNoData;
}
}  // ycsbc

/*
// block_number endpoint
int main(int argc, char **argv){
  std::cout << "Hello world!!" << endl;
  for (int i=0; i<100; i++){
    utils::sleep(0.1);
    cout << "count " << i << endl;
  }
  return 0;
}
*/
