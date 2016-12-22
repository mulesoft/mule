/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.config;

import org.mule.compatibility.core.api.config.ThreadingProfile;
import org.mule.compatibility.core.config.pool.ThreadPoolFactory;
import org.mule.compatibility.core.work.MuleWorkManager;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


public class ImmutableThreadingProfile implements ThreadingProfile {

  private int maxThreadsActive;
  private int maxThreadsIdle;
  private int maxBufferSize;
  private long threadTTL;
  private long threadWaitTimeout;
  private int poolExhaustedAction;
  private boolean doThreading;

  private ThreadPoolFactory poolFactory = ThreadPoolFactory.newInstance();
  private WorkManagerFactory workManagerFactory = new DefaultWorkManagerFactory();
  private RejectedExecutionHandler rejectedExecutionHandler;
  private ThreadFactory threadFactory;
  private MuleContext muleContext;

  public ImmutableThreadingProfile(int maxThreadsActive, int maxThreadsIdle, int maxBufferSize, long threadTTL,
                                   long threadWaitTimeout, int poolExhaustedAction, boolean doThreading,
                                   RejectedExecutionHandler rejectedExecutionHandler, ThreadFactory threadFactory) {
    this.maxThreadsActive = maxThreadsActive;
    this.maxThreadsIdle = maxThreadsIdle;
    this.maxBufferSize = maxBufferSize;
    this.threadTTL = threadTTL;
    this.threadWaitTimeout = threadWaitTimeout;
    this.poolExhaustedAction = poolExhaustedAction;
    this.doThreading = doThreading;
    this.rejectedExecutionHandler = rejectedExecutionHandler;
    this.threadFactory = threadFactory;
  }

  public ImmutableThreadingProfile(ThreadingProfile tp) {
    this(tp.getMaxThreadsActive(), tp.getMaxThreadsIdle(), tp.getMaxBufferSize(), tp.getThreadTTL(), tp.getThreadWaitTimeout(),
         tp.getPoolExhaustedAction(), tp.isDoThreading(), tp.getRejectedExecutionHandler(), tp.getThreadFactory());
  }

  @Override
  public int getMaxThreadsActive() {
    return maxThreadsActive;
  }

  @Override
  public int getMaxThreadsIdle() {
    return maxThreadsIdle;
  }

  @Override
  public long getThreadTTL() {
    return threadTTL;
  }

  @Override
  public long getThreadWaitTimeout() {
    return threadWaitTimeout;
  }

  @Override
  public int getPoolExhaustedAction() {
    return poolExhaustedAction;
  }

  @Override
  public RejectedExecutionHandler getRejectedExecutionHandler() {
    return rejectedExecutionHandler;
  }

  @Override
  public ThreadFactory getThreadFactory() {
    return threadFactory;
  }

  @Override
  public void setMaxThreadsActive(int maxThreadsActive) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setMaxThreadsIdle(int maxThreadsIdle) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setThreadTTL(long threadTTL) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setThreadWaitTimeout(long threadWaitTimeout) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setPoolExhaustedAction(int poolExhaustPolicy) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void setThreadFactory(ThreadFactory threadFactory) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int getMaxBufferSize() {
    return maxBufferSize;
  }

  @Override
  public void setMaxBufferSize(int maxBufferSize) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public WorkManagerFactory getWorkManagerFactory() {
    return workManagerFactory;
  }

  @Override
  public void setWorkManagerFactory(WorkManagerFactory workManagerFactory) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public WorkManager createWorkManager(String name, int shutdownTimeout) {
    return workManagerFactory.createWorkManager(new ImmutableThreadingProfile(this), name, shutdownTimeout);
  }

  @Override
  public ExecutorService createPool() {
    return createPool(null);
  }

  @Override
  public ExecutorService createPool(String name) {
    return poolFactory.createPool(name, new ImmutableThreadingProfile(this));
  }

  @Override
  public boolean isDoThreading() {
    return doThreading;
  }

  @Override
  public void setDoThreading(boolean doThreading) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public ThreadPoolFactory getPoolFactory() {
    return poolFactory;
  }

  @Override
  public ScheduledExecutorService createScheduledPool(String name) {
    return poolFactory.createScheduledPool(name, new ImmutableThreadingProfile(this));
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;

    // propagate mule context
    if (this.workManagerFactory instanceof MuleContextAware) {
      ((MuleContextAware) workManagerFactory).setMuleContext(muleContext);
    }

    poolFactory.setMuleContext(muleContext);
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public String toString() {
    return "ThreadingProfile{" + "maxThreadsActive=" + maxThreadsActive + ", maxThreadsIdle=" + maxThreadsIdle
        + ", maxBufferSize=" + maxBufferSize + ", threadTTL=" + threadTTL + ", poolExhaustedAction=" + poolExhaustedAction
        + ", threadWaitTimeout=" + threadWaitTimeout + ", doThreading=" + doThreading + ", workManagerFactory="
        + workManagerFactory + ", rejectedExecutionHandler=" + rejectedExecutionHandler + ", threadFactory=" + threadFactory
        + "}";
  }

  public static class DefaultWorkManagerFactory implements WorkManagerFactory, MuleContextAware {

    protected MuleContext muleContext;

    @Override
    public WorkManager createWorkManager(ThreadingProfile profile, String name, int shutdownTimeout) {
      final WorkManager workManager = new MuleWorkManager(profile, name, shutdownTimeout);
      if (muleContext != null) {
        MuleContextAware contextAware = (MuleContextAware) workManager;
        contextAware.setMuleContext(muleContext);
      }

      return workManager;
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.muleContext = context;
    }
  }

}
