/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.ack;

import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.test.JmsMessageStorage.pollMessage;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.ACK;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.NOTHING;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.RECOVER;
import static org.mule.test.allure.AllureConstants.JmsFeature.JMS_EXTENSION;

import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.test.JmsMessageStorage;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(JMS_EXTENSION)
@Stories("Manual Acknowledgement over sessions")
public class JmsManualAckTestCase extends JmsAbstractAckTestCase {

  @Override
  public AckMode getAckMode() {
    return MANUAL;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return false;
  }

  @Test
  @Description("Receives two messages which are manually acknowledged and a third one that doesn't. After a session " +
      "recover only the last message get's redelivered")
  public void ackSessionManually() throws Exception {
    publish(buildMessage("This is a message", ACK));
    publish(buildMessage("This is a message", ACK));
    String messageToReDeliver = buildMessage("Message to be re delivered", NOTHING);
    publish(messageToReDeliver);
    validate(() -> JmsMessageStorage.receivedMessages() == 3, 5000, 50);

    String ackId = pollMessage().getAttributes().get().getAckId();
    JmsMessageStorage.cleanUpQueue();
    recoverSession(ackId);
    validate(() -> JmsMessageStorage.receivedMessages() == 1, 5000, 50);
    assertJmsMessage(pollMessage(), messageToReDeliver, true);
  }

  @Test
  @Description("A successfully processed message is not acknowledged and after a session recover it get's redelivered")
  public void recoveredSessionReDeliversNotAcknowledgedMessages() throws Exception {
    String message = buildMessage("Message to recover", RECOVER);
    publish(message);
    assertJmsMessage(pollMessage(), message, false);
    assertJmsMessage(pollMessage(), message, true);
  }
}
