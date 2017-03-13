/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.ack;

import static org.mule.extensions.jms.api.config.AckMode.NONE;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.EXPLODE;
import static org.mule.extensions.jms.test.ack.JmsAbstractAckTestCase.Actions.NOTHING;
import org.junit.Test;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.test.JmsMessageStorage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("JMS Extension")
@Stories("None Acknowledgement over sessions")
public class JmsNoneAckTestCase extends JmsAbstractAckTestCase {

  @Override
  public AckMode getAckMode() {
    return NONE;
  }

  @Test
  @Description("Messages get's acknowledged automatically by the JMS Client")
  public void sessionIsAutomaticallyAck() throws Exception {
    String message = "Message to ACK";
    publish(buildMessage(message, EXPLODE));
    publish(buildMessage(message, NOTHING));

    validate(() -> JmsMessageStorage.receivedMessages() == 2, 5000, 50);
    cleanUpQueues();
    assertQueueIsEmpty();
  }
}
