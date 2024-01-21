Basic example on how race condition between threads can cause unexpected result. 

The code will create multiple workers (threads or goroutine) that conccurently increment X by 1. If synchronization is done correctly, X will be equal to the number of workers at the end, otherwise X will be less than the number of workers indicating lost updates
