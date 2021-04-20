#include "db/corda_db.h"

using namespace std;

namespace ycsbc {

CordaDB::CordaDB(const string &endpoint, const string &wl_name)
: endpoint_(endpoint) {
	if (wl_name == "ycsb") {
		sctype_ = BBUtils::SmartContractType::KVStore;
	} else {
		sctype_ = BBUtils::SmartContractType::DoNothing;
	}
}

// ignore table
// ignore field
// read value indicated by a key
int CordaDB::Read(const string &table, const string &key,
		const vector<string> *fields, vector<KVPair> &result) {
	return DB::kOK;
}

// ignore table
// update value indicated by a key
int CordaDB::Update(const string &table, const string &key,
		vector<KVPair> &values) {

	string val = "";
	for (auto v : values) {
		val += v.first + "=" + v.second + " ";
	}

	char buff[256 + val.length()];

	if (sctype_ == BBUtils::SmartContractType::DoNothing) {

	}
	else {
		sprintf(buff, "%s%s?command=%d&key=%s&val=%s&partyName=PartyA",
				endpoint_.c_str(), API_ENDPOINT, Command::Update, key.c_str(), val.c_str());
		RestClient::Response resp = RestClient::put(buff, REQUEST_HEADERS, "");
	}

	return DB::kOK;
}

// ignore table
// ignore field
// concate values in KVPairs into one long value
int CordaDB::Insert(const string &table, const string &key,
		vector<KVPair> &values) {
	string val = "";
	for (auto v : values) {
		val += v.first + "=" + v.second + " ";
	}
	replace(val.begin(), val.end(), '=', '-');
	replace(val.begin(), val.end(), ' ', '_');

	char buff[256 + val.length()];

	if (sctype_ == BBUtils::SmartContractType::DoNothing) {

	}
	else {
		sprintf(buff, "%s%s?command=%d&key=%s&val=%s&partyName=PartyA",
				endpoint_.c_str(), API_ENDPOINT, Command::Write, key.c_str(), val.c_str());

		RestClient::Response resp = RestClient::put(buff, REQUEST_HEADERS, "");
#ifdef DEBUG
		cerr << "Corda request: " << buff << endl;
		cerr << "Corda response: " << resp.body << endl;
#endif
	}

	return DB::kOK;
}

// ignore table
// delete value indicated by a key
int CordaDB::Delete(const string &table, const string &key) {
	vector<KVPair> empty_val;
	char buff[256];

	if (sctype_ == BBUtils::SmartContractType::DoNothing) {

	}
	else {
		sprintf(buff, "%s%s?command=%d&key=%s&partyName=PartyA",
				endpoint_.c_str(), API_ENDPOINT, Command::Delete, key.c_str());
		RestClient::Response resp = RestClient::put(buff, REQUEST_HEADERS, "");
	}

	return DB::kOK;
}

int CordaDB::GetTip() { return 0; }

// get all tx from the start_block until latest
vector<string> CordaDB::PollTxn(int block_number) {
	std::vector<std::string> ret;
	return ret;
}

}  // ycsbc
