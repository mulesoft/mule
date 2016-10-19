/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.factory;

import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;


public class NonBlockingProcessingStrategyFactory implements ProcessingStrategyFactory {

  protected Integer maxThreads;
  protected Integer minThreads;
  protected Integer maxBufferSize;
  protected Long threadTTL;
  protected Long threadWaitTimeout;
  protected Integer poolExhaustedAction;

  @Override
  public ProcessingStrategy create() {
    final NonBlockingProcessingStrategy processingStrategy = new NonBlockingProcessingStrategy();

    if (maxThreads != null) {
      processingStrategy.setMaxThreads(maxThreads);
    }
    if (minThreads != null) {
      processingStrategy.setMinThreads(minThreads);
    }
    if (maxBufferSize != null) {
      processingStrategy.setMaxBufferSize(maxBufferSize);
    }
    if (threadTTL != null) {
      processingStrategy.setThreadTTL(threadTTL);
    }
    if (threadWaitTimeout != null) {
      processingStrategy.setThreadWaitTimeout(threadWaitTimeout);
    }
    if (poolExhaustedAction != null) {
      processingStrategy.setPoolExhaustedAction(poolExhaustedAction);
    }

    return processingStrategy;
  }

  public Integer getMaxThreads() {
    return maxThreads;
  }

  public void setMaxThreads(Integer maxThreads) {
    this.maxThreads = maxThreads;
  }

  public Integer getMinThreads() {
    return minThreads;
  }

  public void setMinThreads(Integer minThreads) {
    this.minThreads = minThreads;
  }

  public Integer getMaxBufferSize() {
    return maxBufferSize;
  }

  public void setMaxBufferSize(Integer maxBufferSize) {
    this.maxBufferSize = maxBufferSize;
  }

  public Long getThreadTTL() {
    return threadTTL;
  }

  public void setThreadTTL(Long threadTTL) {
    this.threadTTL = threadTTL;
  }

  public Long getThreadWaitTimeout() {
    return threadWaitTimeout;
  }

  public void setThreadWaitTimeout(Long threadWaitTimeout) {
    this.threadWaitTimeout = threadWaitTimeout;
  }

  public Integer getPoolExhaustedAction() {
    return poolExhaustedAction;
  }

  public void setPoolExhaustedAction(Integer poolExhaustedAction) {
    this.poolExhaustedAction = poolExhaustedAction;
  }
}
