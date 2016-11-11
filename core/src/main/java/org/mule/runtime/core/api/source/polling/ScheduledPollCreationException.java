/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.polling;

import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;

/**
 * <p>
 * This exception is thrown if a {@link ScheduledPoll} could not be created.
 * </p>
 *
 * @since 3.5.0
 */
public class ScheduledPollCreationException extends RuntimeException {

  private static final long serialVersionUID = -7745305625297575768L;

  public ScheduledPollCreationException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ScheduledPollCreationException(String s) {
    super(s);
  }
}
