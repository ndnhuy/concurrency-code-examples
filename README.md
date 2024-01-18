Basic example on how race condition between threads can cause unexpected result. 

The code creates multiple threads and each of them will increase X by 1. If synchronization is done correctly, X will be equal to the number of threads at the end, otherwise X will be less than the number of workers indicating lost updates