/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connector;

/**
 * By default, scheduler is always enabled.
 */
public class DefaultSchedulerController implements SchedulerController {

  @Override
  public boolean isPrimarySchedulingInstance() {
    return true;
  }
}
