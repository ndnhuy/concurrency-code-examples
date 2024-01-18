package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

func main() {
	nWorkers := 10

	// concurrently increase x by a number of workers (goroutine) in thread-safe way,
	// expect that the x will be equal to the total of workers (since each worker will increase x by 1)
	start := time.Now()
	x := safeConcurrentIncreaseX(nWorkers)
	elapsed := time.Since(start)
	fmt.Printf("[thread-safe] expected: x = %v, actual x = %v => passed: %v, duration: %v ms\n", nWorkers, x, x == nWorkers, elapsed)

	// do same as above but in unsafe conccurency, we expect lost updates since no synchronization is made between goroutines
	// at the end, x will not be equal to the total of workers
	start = time.Now()
	x = unsafeConcurrentIncreaseX(nWorkers)
	elapsed = time.Since(start)
	fmt.Printf("[thread-unsafe] expected: x = %v, actual x = %v => passed: %v, duration: %v ms\n", nWorkers, x, x == nWorkers, elapsed)
}

func safeConcurrentIncreaseX(nWorkers int) int {
	if nWorkers == 0 {
		nWorkers = 1
	}
	var wg sync.WaitGroup
	var lock sync.Mutex
	wg.Add(nWorkers)
	x := 0
	work := func(workerId int) {
		lock.Lock()
		x = doWork(x, workerId)
		lock.Unlock()
		wg.Done()
	}

	for i := 0; i < nWorkers; i++ {
		go work(i)
	}

	wg.Wait()
	return x
}

func unsafeConcurrentIncreaseX(nWorkers int) int {
	if nWorkers == 0 {
		nWorkers = 1
	}
	var wg sync.WaitGroup
	wg.Add(nWorkers)
	x := 0
	work := func(workerId int) {
		x = doWork(x, workerId)
		wg.Done()
	}

	for i := 0; i < nWorkers; i++ {
		go work(i)
	}

	wg.Wait()
	return x
}

func doWork(x int, workerId int) int {
	doLongFakeWork(workerId)
	newX := x + 1
	return newX
}

func doLongFakeWork(workerId int) {
	rand := randomDuration()
	// fmt.Printf("worker %v started, done in %vms\n", workerId, rand.Milliseconds())
	time.Sleep(rand)
}

func randomDuration() time.Duration {
	return time.Duration(rand.Int63n(1e7))
}
