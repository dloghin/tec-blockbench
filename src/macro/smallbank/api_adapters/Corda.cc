#include <restclient-cpp/restclient.h>
#include "Corda.h"

string CordaDriver::get_txn_hash(string response) {
	return "";
}

void CordaDriver::send_cmd(Command command, unsigned acc1, unsigned acc2,unsigned amount) {
	double start_time = time_now();
	char buff[256];
	sprintf(buff, "%s%s?command=%d&account1=%d&account2=%d&amount=%d&partyName=PartyB",
			endpoint_.c_str(), API_ENDPOINT, command, acc1, acc2, amount);
	RestClient::Response resp = RestClient::put(buff, REQUEST_HEADERS, "");
#ifdef DEBUG
	std::cout << "Corda response: " << resp.body << std::endl;
#endif
	string txn_hash = get_txn_hash(resp.body);
	txlock_->lock();
	(*pendingtx_)[txn_hash] = start_time;
	txlock_->unlock();
}

void CordaDriver::Amalgate(unsigned acc1, unsigned acc2) {
	send_cmd(Command::Amalgate, acc1, acc2, 0);
}

void CordaDriver::GetBalance(unsigned acc) {
	send_cmd(Command::GetBalance, acc, 0, 0);
}

void CordaDriver::UpdateBalance(unsigned acc, unsigned amount) {
	send_cmd(Command::UpdateBalance, acc, 0, amount);
}

void CordaDriver::UpdateSaving(unsigned acc, unsigned amount) {
	send_cmd(Command::UpdateSaving, acc, 0, amount);
}

void CordaDriver::SendPayment(unsigned acc1, unsigned acc2, unsigned amount) {
	send_cmd(Command::SendPayment, acc1, acc2, amount);
}

void CordaDriver::WriteCheck(unsigned acc, unsigned amount) {
	send_cmd(Command::WriteCheck, acc, 0, amount);
}

void CordaDriver::deploy(const std::string& dbname, const std::string& endpoint) {}

void CordaDriver::add_to_queue(string response){
  double start_time = time_now();
  string txn_hash = get_txn_hash(response);
  txlock_->lock();
  (*pendingtx_)[txn_hash] = start_time;
  txlock_->unlock();
}

unsigned int CordaDriver::get_tip_block_number() {
	return 0;
}

vector<string> CordaDriver::poll_tx(int block_number) {
	vector<string> txns;
	return txns;
}
