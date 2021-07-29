package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"

	"fmt"
	"os"
	"strconv"
	"time"
)

func main() {
	if len(os.Args) != 3 {
                fmt.Printf("Usage: %s <sign|verify> <size>\n", os.Args[0])
                return
        }

        algo := os.Args[1]
        if algo != "sign" && algo != "verify" {
                fmt.Printf("Wrong algorithm %s\n", algo)
                return
        }

	n, err := strconv.Atoi(os.Args[2])
        if err != nil {
                fmt.Println("Invalid size", os.Args[1])
                return
        }

	msg := make([]byte, n)
	for i := 0; i < n; i++ {
		msg[i] = byte(i)
	}
	fmt.Printf("Message size %v\n", n)

	iter := 100000
	fmt.Printf("Iterations %v\n", iter)

	// Generate a key
	lowLevelKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		fmt.Println(err)
		return
	}

	if algo == "sign" {
		start := time.Now()
		for i := 0; i < iter; i++ {
			// r, s, err := 
		        ecdsa.Sign(rand.Reader, lowLevelKey, msg)
		}
		elapsed := time.Since(start)
		fmt.Printf("%s time %v\n", algo, elapsed)
	} else {
		r, s, _ := ecdsa.Sign(rand.Reader, lowLevelKey, msg)
		start := time.Now()
		for i := 0; i < 100000; i++ {
			ecdsa.Verify(&lowLevelKey.PublicKey, msg, r, s)
		}
		elapsed := time.Since(start)
                fmt.Printf("%s time %v\n", algo, elapsed)
	}
}

