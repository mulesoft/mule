/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.ack;

import static org.mule.extensions.jms.api.config.AckMode.AUTO;
import static org.mule.extensions.jms.test.JmsMessageStorage.pollMessage;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.EXPLODE;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.NOTHING;
import org.junit.Test;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.test.JmsMessageStorage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("JMS Extension")
@Stories("Manual Acknowledgement over sessions")
public class JmsAutoAckTestCase extends JmsAbstractAckTestCase {

  @Override
  public AckMode getAckMode() {
    return AUTO;
  }

  @Test
  @Description("Messages are consumed correctly and get's acknowledged automatically by the JMS Client")
  public void sessionIsAutomaticallyAckOnSuccessFlow() throws Exception {
    String message = "Message to ACK";
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));
    publish(buildMessage(message, NOTHING));


    validate(() -> JmsMessageStorage.receivedMessages() == 8, 5000, 50);
    cleanUpQueues();
    assertQueueIsEmpty();
  }

  @Test
  @Description("Messages that fails produces a session recover and get's redelivered")
  public void sessionIsAutomaticallyRecoverOnErrorFlow() throws Exception {
    String message = buildMessage("Message to be redelivered", EXPLODE);
    publish(message);
    validate(() -> JmsMessageStorage.receivedMessages() == 2, 5000, 50);
    assertJmsMessage(pollMessage(), message, false);
    assertJmsMessage(pollMessage(), message, true);
  }
}
