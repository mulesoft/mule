/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.threading;

import static com.google.common.base.Preconditions.checkNotNull;

import org.mule.runtime.api.profiling.type.context.OperationThreadSnapshot;

public final class DefaultOperationThreadSnapshot implements OperationThreadSnapshot {

  private final Long blockedTime;
  private final Long waitedTime;
  private final Long cpuTime;

  private DefaultOperationThreadSnapshot(Long blockedTime, Long waitedTime, Long cpuTime) {
    this.blockedTime = blockedTime;
    this.waitedTime = waitedTime;
    this.cpuTime = cpuTime;
  }

  @Override
  public Long getBlockedTime() {
    return blockedTime;
  }

  @Override
  public Long getWaitedTime() {
    return waitedTime;
  }

  @Override
  public Long getCpuTime() {
    return cpuTime;
  }

  public static class Builder {

    private Long blockedTime = null;
    private Long waitedTime = null;
    private Long cpuTime = null;

    public DefaultOperationThreadSnapshot build() {
      checkNotNull(blockedTime);
      checkNotNull(waitedTime);
      checkNotNull(cpuTime);
      return new DefaultOperationThreadSnapshot(blockedTime, waitedTime, cpuTime);
    }

    Builder withBlockedTime(Long pBlockedTime) {
      this.blockedTime = pBlockedTime;
      return this;
    }

    Builder withWaitedTime(Long pWaitedTime) {
      this.waitedTime = pWaitedTime;
      return this;
    }

    Builder withCPUTime(Long pCpuTime) {
      this.cpuTime = pCpuTime;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
