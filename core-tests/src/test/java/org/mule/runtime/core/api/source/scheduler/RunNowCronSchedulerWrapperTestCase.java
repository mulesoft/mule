package org.mule.runtime.core.api.source.scheduler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.tck.size.SmallTest;

import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

@SmallTest
public class RunNowCronSchedulerWrapperTestCase {

    private Integer count;

    @Test
    public void wrapperRunsTwice() throws Exception {
        count = 0;
        CronScheduler cronScheduler = new CronScheduler();
        cronScheduler.setExpression("0/30 0/1 * 1/1 * ? *");
        cronScheduler.setTimeZone("GMT");
        RunNowCronSchedulerWrapper schedulerWrapper = new RunNowCronSchedulerWrapper(cronScheduler);
        Scheduler executor = new TestScheduler(1, "custom", true);
        schedulerWrapper.doSchedule(executor, () -> {
            count++;
        });
        executor.shutdown();
        if (executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
            assertThat(count, equalTo(2));
        } else {
            fail("The executor took too long to run the scheduled tasks.");
        }
    }

    private class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

        private String threadNamePrefix;
        private ExecutorService executor;

        public TestScheduler(int threads, String threadNamePrefix, boolean reject) {
            super(1, new NamedThreadFactory(threadNamePrefix + ".tasks"));
            this.threadNamePrefix = threadNamePrefix;
            executor = new ThreadPoolExecutor(threads, threads, 0l, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue(reject ? threads : Integer.MAX_VALUE),
                    new NamedThreadFactory(threadNamePrefix));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return executor.submit(task);
        }

        @Override
        public Future<?> submit(Callable task) {
            return executor.submit(task);
        }

        @Override
        public void stop() {
            shutdown();
            executor.shutdown();
        }

        @Override
        public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
            command.run();
            return new NullScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
            command.run();
            return new NullScheduledFuture();
        }

        @Override
        public String getName() {
            return threadNamePrefix;
        }

    }

    private class NullScheduledFuture<V> implements ScheduledFuture<V> {

        private NullScheduledFuture() {
            // Nothing to do
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

}