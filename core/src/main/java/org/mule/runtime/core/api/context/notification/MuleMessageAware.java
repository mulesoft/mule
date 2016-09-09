/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.api.InternalMessage;

/**
 * Signals that a Notification can have a {@link Message} available through it. The message set will be the one available when the
 * notification was triggered.
 *
 * @see org.mule.runtime.core.api.InternalMessage
 */
public interface MuleMessageAware {

  public InternalMessage getMessage();
}
