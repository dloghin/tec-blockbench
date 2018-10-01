#ifndef SMARTCONTRACT_DRIVERS_SMALLBANK__CORDA_H_
#define SMARTCONTRACT_DRIVERS_SMALLBANK__CORDA_H_

#include "DB.h"
#include "utils/timer.h"
#include "utils/utils.h"

#define API_ENDPOINT "/api/smallbank/smallbank"

const std::string REQUEST_HEADERS = "application/json";

enum Command { None, Amalgate, GetBalance, UpdateBalance, UpdateSaving, SendPayment, WriteCheck };

class CordaDriver : public DB {

public:
	void Amalgate(unsigned acc1, unsigned acc2);
	void GetBalance(unsigned acc);
	void UpdateBalance(unsigned acc, unsigned amount);
	void UpdateSaving(unsigned acc, unsigned amount);
	void SendPayment(unsigned acc1, unsigned acc2, unsigned amount);
	void WriteCheck(unsigned acc, unsigned amount);

	static CordaDriver* GetInstance(std::string dbname, std::string endpoint) {
		static CordaDriver sb;
		return &sb;
	}

	CordaDriver() {}
	CordaDriver(std::string path, std::string endpoint) {}

	void Init(unordered_map<string, double> *pendingtx, SpinLock *lock){
		pendingtx_ = pendingtx;
		txlock_ = lock;
	}

	~CordaDriver() {}

	unsigned int get_tip_block_number();
	vector<string> poll_tx(int block_number);

private:
	std::string endpoint_, from_address_, to_address_;

	void send_cmd(Command command, unsigned acc1, unsigned acc2,unsigned amount);
	string get_txn_hash(string response);

	void deploy(const std::string& dbname, const std::string& endpoint);
	void add_to_queue(string json);

	unordered_map<string, double> *pendingtx_;
	SpinLock *txlock_;
};

#endif /* SMARTCONTRACT_DRIVERS_SMALLBANK__CORDA_H_ */
