/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.notification;

import org.mule.functional.api.component.FunctionalTestComponent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.context.notification.CustomNotification;

/**
 * A <code>FunctionlTestNotification</code> is fired by the {@link FunctionalTestComponent} when it receives an event. Test cases
 * can register a {@link FunctionalTestNotificationListener} with Mule to receive these notifications and make assertions about
 * the number of messages received or the content of the message.
 * <p/>
 * This Notification contains the current {@link Event}, {@link FlowConstruct} and reply message. The resource Identifier for this
 * event is the service name that received the message. This means you can register to listen to Notifications from a selected
 * {@link FunctionalTestComponent}. i.e. <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 * <p/>
 * This registration would only receive {@link FunctionalTestNotification} objects from components called 'MyJmsTestComponent' and
 * 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * @see FunctionalTestComponent
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
  private final Event event;

  public FunctionalTestNotification(Event event, FlowConstruct flowConstruct, Object replyMessage, int action)
      throws TransformerException {
    super(event.getMessage().getPayload().getValue(), action);
    resourceIdentifier = flowConstruct.getName();
    this.replyMessage = replyMessage;
    this.event = event;
  }

  public Object getReplyMessage() {
    return replyMessage;
  }

  public Event getEvent() {
    return event;
  }
}
