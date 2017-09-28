/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.scheduler.Scheduler;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleUnitTestSupportLifecycleSchedulerDecorator implements Scheduler {

  private static final Logger LOGGER = getLogger(SimpleUnitTestSupportLifecycleSchedulerDecorator.class);

  private String name;
  private Scheduler decorated;
  private SimpleUnitTestSupportSchedulerService ownerService;
  private boolean stopped;

  private Collection<ScheduledFuture> recurrentTasks = new LinkedList<>();

  public SimpleUnitTestSupportLifecycleSchedulerDecorator(String name, Scheduler decorated,
                                                          SimpleUnitTestSupportSchedulerService ownerService) {
    super();
    this.name = name;
    this.decorated = decorated;
    this.ownerService = ownerService;
  }

  @Override
  public void stop() {
    List<ScheduledFuture> stillaCtiveRecurrentTasks = recurrentTasks.stream()
        .filter(recurrentTask -> !(recurrentTask.isDone() || recurrentTask.isCancelled())).collect(toList());
    if (!stillaCtiveRecurrentTasks.isEmpty()) {
      LOGGER.warn("Scheduler '" + name + "' stopped while it still has active recurrent tasks:"
          + stillaCtiveRecurrentTasks.toString());
    }
    stillaCtiveRecurrentTasks.forEach(recurrentTask -> recurrentTask.cancel(true));

    this.stopped = true;
    decorated.stop();
    ownerService.stoppedScheduler(this);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return decorated.schedule(wrap(command), delay, unit);
  }

  @Override
  public void execute(Runnable command) {
    decorated.execute(wrap(command));
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return decorated.schedule(wrap(callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    ScheduledFuture<?> recurrentTask = decorated.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
    recurrentTasks.add(recurrentTask);
    return recurrentTask;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    ScheduledFuture<?> recurrentTask = decorated.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
    recurrentTasks.add(recurrentTask);
    return recurrentTask;
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    ScheduledFuture<?> recurrentTask = decorated.scheduleWithCronExpression(wrap(command), cronExpression);
    recurrentTasks.add(recurrentTask);
    return recurrentTask;
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    ScheduledFuture<?> recurrentTask = decorated.scheduleWithCronExpression(wrap(command), cronExpression, timeZone);
    recurrentTasks.add(recurrentTask);
    return recurrentTask;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return decorated.submit(wrap(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return decorated.submit(wrap(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return decorated.submit(wrap(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return decorated.invokeAll(tasks.stream().map(t -> wrap(t)).collect(toList()));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return decorated.invokeAll(tasks.stream().map(t -> wrap(t)).collect(toList()), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return decorated.invokeAny(tasks.stream().map(t -> wrap(t)).collect(toList()));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return decorated.invokeAny(tasks.stream().map(t -> wrap(t)).collect(toList()), timeout, unit);
  }

  private Runnable wrap(Runnable command) {
    return () -> {
      try {
        command.run();
      } catch (Throwable t) {
        LOGGER.error(format("Task '%s' finished with exception in test scheduler '%s'", command.toString(), decorated.getName()),
                     t);
      }
    };
  }

  private <V> Callable<V> wrap(Callable<V> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Throwable t) {
        LOGGER.error(format("Task '%s' finished with exception in test scheduler '%s'", callable.toString(), decorated.getName()),
                     t);
        return null;
      }
    };
  }

  @Override
  public void shutdown() {
    this.stopped = true;
    decorated.shutdown();
    ownerService.stoppedScheduler(this);
  }

  @Override
  public List<Runnable> shutdownNow() {
    this.stopped = true;
    final List<Runnable> cancelledJobs = decorated.shutdownNow();
    ownerService.stoppedScheduler(this);
    return cancelledJobs;
  }

  @Override
  public boolean isShutdown() {
    return this.stopped || decorated.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return decorated.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return decorated.awaitTermination(timeout, unit);
  }

  @Override
  public String getName() {
    return SimpleUnitTestSupportLifecycleSchedulerDecorator.class.getSimpleName() + ":" + decorated.getName() + "(" + name + ")";
  }

  @Override
  public String toString() {
    return getName();
  }
}
