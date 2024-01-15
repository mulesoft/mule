/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.retry.internal;


import org.mule.runtime.retry.api.policy.PolicyStatus;

/**
 * Default implementation of {@link PolicyStatus}.
 */
public final class DefaultPolicyStatus implements PolicyStatus {

  private boolean exhausted = false;
  private boolean ok = false;
  private Throwable throwable;

  public DefaultPolicyStatus() {
    this.ok = true;
  }

  public DefaultPolicyStatus(boolean exhausted, Throwable throwable) {
    this.exhausted = exhausted;
    this.throwable = throwable;
  }

  public boolean isExhausted() {
    return exhausted;
  }

  public boolean isOk() {
    return ok;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public String toString() {
    return "PolicyStatus{ ok: " + ok + "; exhausted: " + exhausted + "; throwable: " + throwable + "}";
  }
}
