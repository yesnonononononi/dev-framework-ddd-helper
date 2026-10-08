package com.summit.ddd.utils;


import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class RetryUtil {
    private static final long ABSOLUTE_MAX_DELAY_MILLIS = 60_000L;

    /**
     * Retry the task with exponential backoff.
     *
     * @param basicTimeout       The basic timeout for each retry attempt.
     * @param maxRetry           The maximum number of retry attempts.
     * @param multiple           The multiple for exponential backoff.
     * @param maxDelay           The maximum delay between retries. The unit is same as timeUnit.
     * @param timeUnit           The time unit for the timeout.
     * @param task               The task to be executed.
     * @param exceptionPredicate The predicate to determine whether to retry on a given exception. All exception will be retried if this parameter is null.
     * @return The result of the task.
     */
    public static <T> T retry(long basicTimeout,
                              int maxRetry,
                              int multiple,
                              long maxDelay,
                              TimeUnit timeUnit,
                              Callable<T> task,
                              Predicate<Throwable> exceptionPredicate
    ) throws Exception {
        int retryCount = 0;

        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(timeUnit, "Time unit cannot be null");
        multiple = Math.max(multiple, 1);
        basicTimeout = Math.max(basicTimeout, 0);
        maxRetry = Math.max(maxRetry, 0);


        while (retryCount <= maxRetry) {
            try {
                return task.call();
            } catch (Exception e) {
                if (exceptionPredicate != null && !exceptionPredicate.test(e)) {
                    throw e;
                }

                if (retryCount >= maxRetry) {
                      throw e;
                }

                long capped = Math.min((long) (basicTimeout * Math.pow(multiple, retryCount)), maxDelay);

                long timeout = capped < 0 ? 0 : ThreadLocalRandom.current().nextLong(0, capped + 1);

                retryCount++;

                try {
                    long millis = Math.min(timeUnit.toMillis(timeout), ABSOLUTE_MAX_DELAY_MILLIS);

                    Thread.sleep(millis);

                } catch (InterruptedException ie) {

                    Thread.currentThread().interrupt();

                    throw ie;
                }

            }
        }
        throw new IllegalStateException("unreachable");
    }

    /**
     * Retry the task with exponential backoff.
     *
     * @param basicTimeout The basic timeout for each retry attempt.
     * @param maxRetry     The maximum number of retry attempts.
     * @param multiple     The multiple for exponential backoff.
     * @param timeUnit     The time unit for the timeout.
     * @param task         The task to be executed.
     * @return The result of the task.
     */
    public static <T> T retry(long basicTimeout,
                              int maxRetry,
                              int multiple,
                              TimeUnit timeUnit,
                              Callable<T> task
    ) throws Exception {
        return retry(basicTimeout, maxRetry, multiple, ABSOLUTE_MAX_DELAY_MILLIS, timeUnit, task, null);
    }

    public static <T> T retry(long basicTimeout,
                              int maxRetry,
                              int multiple,
                              long maxDelay,
                              TimeUnit timeUnit,
                              Callable<T> task
    ) throws Exception {
        return retry(basicTimeout, maxRetry, multiple, maxDelay, timeUnit, task, null);
    }
}
