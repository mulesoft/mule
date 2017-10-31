/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.notification;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.TransformerException;

/**
 * A <code>FunctionlTestNotification</code> is fired by the {@link FunctionalTestProcessor} when it receives an event. Test cases
 * can register a {@link FunctionalTestNotificationListener} with Mule to receive these notifications and make assertions about
 * the number of messages received or the content of the message.
 * <p/>
 * This Notification contains the current {@link CoreEvent}, {@link FlowConstruct} and reply message. The resource Identifier for
 * this event is the service name that received the message. This means you can register to listen to Notifications from a
 * selected {@link FunctionalTestProcessor}. i.e. <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 * <p/>
 * This registration would only receive {@link FunctionalTestNotification} objects from components called 'MyJmsTestComponent' and
 * 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * @see FunctionalTestProcessor
 * @see FunctionalTestNotificationListener
 * @see org.mule.runtime.core.api.MuleContext
 */
public class FunctionalTestNotification extends CustomNotification {

  /** Serial version */
  private static final long serialVersionUID = -3435373745940904597L;

  public static final int EVENT_RECEIVED = -999999;

  static {
    registerAction("event received", EVENT_RECEIVED);
  }

  private final Object replyMessage;
  private final Message message;

  public FunctionalTestNotification(Message message, String flowName, Object replyMessage, int action)
      throws TransformerException {
    super(message.getPayload().getValue(), action);
    resourceIdentifier = flowName;
    this.replyMessage = replyMessage;
    this.message = message;
  }

  public Object getReplyMessage() {
    return replyMessage;
  }

  public Message getMessage() {
    return message;
  }

  @Override
  public String getEventName() {
    return "FunctionalTestNotification";
  }
}
