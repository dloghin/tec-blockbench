package main

import (    
    "crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"github.com/ConsenSys/quorum/crypto/secp256k1"

	"fmt"
	"io"
	"os"
	"strconv"
	"time"
)

func generateKeyPair() (pubkey, privkey []byte) {
	key, err := ecdsa.GenerateKey(secp256k1.S256(), rand.Reader)
	if err != nil {
		panic(err)
	}
	pubkey = elliptic.Marshal(secp256k1.S256(), key.X, key.Y)

	privkey = make([]byte, 32)
	blob := key.D.Bytes()
	copy(privkey[32-len(blob):], blob)

	return pubkey, privkey
}

func csprngEntropy(n int) []byte {
	buf := make([]byte, n)
	if _, err := io.ReadFull(rand.Reader, buf); err != nil {
		panic("reading from crypto/rand failed: " + err.Error())
	}
	return buf
}

func main() {
	if len(os.Args) != 2 {
       fmt.Printf("Usage: %s <size>\n", os.Args[0])
       return
    }        

	n, err := strconv.Atoi(os.Args[1])
    if err != nil {
       fmt.Println("Invalid size", os.Args[1])
       return
    }

	msg := csprngEntropy(n)
	fmt.Printf("Message size %v\n", n)

	iter := 100000000
	fmt.Printf("Iterations %v\n", iter)

	// Generate a key
	_, seckey := generateKeyPair()

	
    start := time.Now()
    for i := 0; i < iter; i++ {
       secp256k1.Sign(msg, seckey)
    }
	elapsed := time.Since(start)
	fmt.Printf("sign time %v\n", elapsed)	
}

