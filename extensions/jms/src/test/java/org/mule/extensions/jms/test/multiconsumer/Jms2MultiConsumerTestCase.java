/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.multiconsumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extensions.jms.api.destination.DestinationType.TOPIC;
import static org.mule.extensions.jms.test.JmsMessageStorage.receivedMessages;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Ignore("This should be enabled once JMS 2.0 is enabled for testing")
@Features("JMS Extension")
@Stories("Multi Consumers - JMS 2.0")
public class Jms2MultiConsumerTestCase extends AbstractJmsMultiConsumerTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"multiconsumer/jms-2.0-multi-consumers.xml", "config/generic/jms_20/jndi-destinations-always.xml"};
  }

  @Test
  public void JMS2TopicsWithSharedConsumersCanUseMultiConsumers() throws Exception {
    publishTo(NUMBER_OF_MESSAGES, topicDestination.getValue(), TOPIC);

    //This is to check that we don't receive repeated messages
    new PollingProber(5000, 100).check(new JUnitLambdaProbe(() -> receivedMessages() == NUMBER_OF_MESSAGES));

    long distinctAckIds = getMessages(NUMBER_OF_MESSAGES)
        .map(result -> result.getAttributes().get().getAckId())
        .distinct()
        .count();

    assertThat(distinctAckIds, is(Long.valueOf(NUMBER_OF_CONSUMERS)));
  }
}
