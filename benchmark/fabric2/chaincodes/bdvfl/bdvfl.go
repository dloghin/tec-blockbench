package main

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"sync/atomic"

	"github.com/ethereum/go-ethereum/crypto/secp256k1"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

type BDVFL struct {
	contractapi.Contract
	txid atomic.Uint64
}

type BDVFLTask struct {
	TaskId       uint64   `json:"taskid"`
	ModelName    string   `json:"modelname"`
	ModelHash    string   `json:"modelhash"`
	Rounds       uint64   `json:"rounds"`
	NodesPKs     []string `json:"nodespks"`
	AggregatorPK string   `json:"aggregatorpk"`
}

func (t *BDVFL) getTask(ctx contractapi.TransactionContextInterface, taskId string) (*BDVFLTask, error) {
	val, err := ctx.GetStub().GetState(taskId)
	if err != nil {
		return nil, err
	}
	task := new(BDVFLTask)
	err = json.Unmarshal(val, task)
	if err != nil {
		return nil, err
	}
	return task, nil
}

func (t *BDVFL) InitLedger(ctx contractapi.TransactionContextInterface) error {
	t.txid.Store(0)
	return nil
}

func (t *BDVFL) CreateBDVFLTask(ctx contractapi.TransactionContextInterface, taskStr string) (string, error) {
	task := new(BDVFLTask)
	err := json.Unmarshal([]byte(taskStr), task)
	if err != nil {
		return "", err
	}
	task.TaskId = t.txid.Add(1)
	var key = strconv.FormatUint(task.TaskId, 10)
	taskAsBytes, _ := json.Marshal(task)
	err = ctx.GetStub().PutState(key, taskAsBytes)
	if err != nil {
		return "", err
	}
	return key, nil
}

func (t *BDVFL) GetInitialModelHash(ctx contractapi.TransactionContextInterface, taskId string) (string, error) {
	task, err := t.getTask(ctx, taskId)
	if err != nil {
		return "", err
	}
	return task.ModelHash, nil
}

func (t *BDVFL) SetPartialParamsHash(ctx contractapi.TransactionContextInterface, taskId string, nodePublicKeyBase64 string, nodeSigBase64 string, round uint64, paramsHash string) error {
	// verify node PK
	task, err := t.getTask(ctx, taskId)
	if err != nil {
		return err
	}
	validNode := false
	for _, node := range task.NodesPKs {
		if node == nodePublicKeyBase64 {
			validNode = true
			break
		}
	}
	if !validNode {
		return errors.New("Invalid node or node PK")
	}

	// verify signature
	payload := taskId + nodePublicKeyBase64 + strconv.FormatUint(round, 10) + paramsHash
	digest := sha256.Sum256([]byte(payload))
	nodePKBytes, err := base64.StdEncoding.DecodeString(nodePublicKeyBase64)
	if err != nil {
		return err
	}
	nodeSigBytes, err := base64.StdEncoding.DecodeString(nodeSigBase64)
	if err != nil {
		return err
	}
	if !secp256k1.VerifySignature(nodePKBytes, digest[:], nodeSigBytes) {
		return errors.New("Invalid signature")
	}

	// verify it was not saved before
	key := taskId + "_" + strconv.FormatUint(round, 10) + "_" + nodePublicKeyBase64
	val, err := ctx.GetStub().GetState(key)
	if val != nil && err != nil {
		return errors.New("Partial parameters hash is already set")
	}

	// save
	err = ctx.GetStub().PutState(key, []byte(paramsHash))
	return err
}

func (t *BDVFL) GetPartialParamsHash(ctx contractapi.TransactionContextInterface, taskId string, nodePublicKeyBase64 string, round uint64) (string, error) {
	key := taskId + "_" + strconv.FormatUint(round, 10) + "_" + nodePublicKeyBase64
	val, err := ctx.GetStub().GetState(key)
	if val == nil && err == nil {
		return "", errors.New("No such partial parameters hash")
	}
	if err != nil {
		return "", err
	}
	return string(val), nil
}

func (t *BDVFL) SetPartialModelHash(ctx contractapi.TransactionContextInterface, taskId string, round uint64, hash string, aggregatorSig string) error {
	task, err := t.getTask(ctx, taskId)
	if err != nil {
		return err
	}
	// verify signature
	payload := taskId + strconv.FormatUint(round, 10) + hash
	digest := sha256.Sum256([]byte(payload))
	nodePKBytes, err := base64.StdEncoding.DecodeString(task.AggregatorPK)
	if err != nil {
		return err
	}
	nodeSigBytes, err := base64.StdEncoding.DecodeString(aggregatorSig)
	if err != nil {
		return err
	}
	if !secp256k1.VerifySignature(nodePKBytes, digest[:], nodeSigBytes) {
		return errors.New("Invalid signature")
	}
	key := taskId + "_" + strconv.FormatUint(round, 10) + "_model"
	val, err := ctx.GetStub().GetState(key)
	if val != nil && err != nil {
		return errors.New("Model hash is already saved")
	}
	return ctx.GetStub().PutState(key, []byte(hash))
}

func (t *BDVFL) GetPartialModelHash(ctx contractapi.TransactionContextInterface, taskId string, round uint64) (string, error) {
	key := taskId + "_" + strconv.FormatUint(round, 10) + "_model"
	val, err := ctx.GetStub().GetState(key)
	if val == nil && err == nil {
		return "", errors.New("No hash for partial model")
	}
	return string(val), nil
}

func main() {
	chaincode, err := contractapi.NewChaincode(new(BDVFL))
	if err != nil {
		fmt.Printf("Error starting BDVFL: %s", err)
	}
	if err := chaincode.Start(); err != nil {
		fmt.Printf("Error starting BDVFL: %s", err.Error())
	}
}
