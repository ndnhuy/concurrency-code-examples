import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

class Main {

  private static final ExecutorService testRunnerExec = Executors.newFixedThreadPool(
    5
  );

  private static final ExecutorService serviceExec = Executors.newFixedThreadPool(
    5
  );

  public static void main(String[] args) throws InterruptedException {
    // the pass rate should be 100% since the class uses synchronization to increment x
    runTest(SafeXIncrement::new);
    // the pass rate should be less than 100% since the class does not synchronize the code that increments x
    runTest(UnsafeXIncrement::new);
    testRunnerExec.shutdown();
    serviceExec.shutdown();
  }

  static void runTest(Supplier<XIncrement> factory)
    throws InterruptedException {
    final int testCnt = 100;
    final AtomicLong success = new AtomicLong();
    final CountDownLatch waitGroup = new CountDownLatch(testCnt);
    Runnable testRunner = () -> {
      boolean passed;
      try {
        passed = concurrencyTestRun(factory.get());
        if (passed) {
          success.incrementAndGet();
        }
      } catch (InterruptedException ignored) {
        // do nothing
      } finally {
        waitGroup.countDown();
      }
    };
    LocalTime start = LocalTime.now();
    for (int i = 0; i < testCnt; i++) {
      testRunnerExec.execute(testRunner);
    }
    waitGroup.await();
    LocalTime end = LocalTime.now();
    double r = success.intValue() * 100 * 1.0 / testCnt;
    System.out.printf(
      "passed rate: %s%s, elapse: %s ms\n",
      Double.toString(r),
      "%",
      Duration.between(start, end).toMillis(),
      "ms"
    );
  }

  static boolean concurrencyTestRun(XIncrement xIncrement)
    throws InterruptedException {
    int nWorkers = 1000;
    CountDownLatch endGate = new CountDownLatch(nWorkers);
    CountDownLatch startGate = new CountDownLatch(1);

    for (int i = 0; i < nWorkers; i++) {
      serviceExec.execute(() -> {
        try {
          startGate.await();
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }
        xIncrement.increment();
        endGate.countDown();
      });
    }

    startGate.countDown();
    endGate.await();
    return xIncrement.getX() == nWorkers;
  }

  static interface XIncrement {
    long getX();
    void increment();
  }

  static class SafeXIncrement implements XIncrement {

    private long x;

    public long getX() {
      return x;
    }

    public void increment() {
      synchronized (this) {
        x = x + 1;
      }
    }
  }

  static class UnsafeXIncrement implements XIncrement {

    private long x;

    public long getX() {
      return x;
    }

    public void increment() {
      x = x + 1;
    }
  }
}
