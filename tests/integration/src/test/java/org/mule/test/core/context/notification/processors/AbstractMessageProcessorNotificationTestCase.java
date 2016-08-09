/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import org.mule.test.core.context.notification.AbstractNotificationTestCase;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

public abstract class AbstractMessageProcessorNotificationTestCase extends AbstractNotificationTestCase {

  protected RestrictedNode pre() {
    return new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
  }

  protected RestrictedNode post() {
    return new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
  }

  protected RestrictedNode prePost() {
    return new Node().serial(pre()).serial(post());
  }
}
