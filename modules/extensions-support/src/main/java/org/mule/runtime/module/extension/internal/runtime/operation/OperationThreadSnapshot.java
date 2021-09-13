/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class OperationThreadSnapshot {

  private final Long blockedTime;
  private final Long waitedTime;
  private final Long cpuTime;

  private OperationThreadSnapshot(Long blockedTime, Long waitedTime, Long cpuTime) {
    this.blockedTime = blockedTime;
    this.waitedTime = waitedTime;
    this.cpuTime = cpuTime;
  }

  /**
   * {@link ThreadInfo#getBlockedTime()}
   * 
   * @return
   */
  public Long getBlockedTime() {
    return blockedTime;
  }

  /**
   * {@link ThreadInfo#getWaitedTime()}
   * 
   * @return
   */
  public Long getWaitedTime() {
    return waitedTime;
  }

  /**
   * {@link ThreadMXBean#getThreadCpuTime(long)}
   * 
   * @return
   */
  public Long getCpuTime() {
    return cpuTime;
  }

  public static class Builder {

    private Long blockedTime = null;
    private Long waitedTime = null;
    private Long cpuTime = null;

    public OperationThreadSnapshot build() {
      checkNotNull(blockedTime);
      checkNotNull(waitedTime);
      checkNotNull(cpuTime);
      return new OperationThreadSnapshot(blockedTime, waitedTime, cpuTime);
    }

    public Builder withBlockedTime(Long blockedTime) {
      this.blockedTime = blockedTime;
      return this;
    }

    public Builder withWaitedTime(Long waitedTime) {
      this.waitedTime = waitedTime;
      return this;
    }

    public Builder withCPUTime(Long cpuTime) {
      this.cpuTime = cpuTime;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
