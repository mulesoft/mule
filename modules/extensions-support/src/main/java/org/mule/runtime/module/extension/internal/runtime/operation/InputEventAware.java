/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * A component that's aware of its input {@link CoreEvent}
 *
 * @since 4.8.0
 */
public interface InputEventAware {

  /**
   * @return the event prior to its execution
   */
  CoreEvent getOriginalEvent();
}
