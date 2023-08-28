/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.queue;

import org.mule.api.annotation.NoImplement;

/**
 * Queue configuration attributes
 */
@NoImplement
public interface QueueConfiguration {

  int MAXIMUM_CAPACITY = 0;

  boolean isPersistent();

  int getCapacity();

}
