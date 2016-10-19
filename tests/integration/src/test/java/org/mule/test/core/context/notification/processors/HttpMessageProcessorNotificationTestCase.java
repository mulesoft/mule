/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.NON_BLOCKING_PROCESSING_STRATEGY;

import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class HttpMessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase {

  @Rule
  public DynamicPort proxyPort = new DynamicPort("port");

  @Rule
  public SystemProperty systemProperty;


  public HttpMessageProcessorNotificationTestCase(boolean nonBlocking) {
    if (nonBlocking) {
      systemProperty = new SystemProperty(MULE_DEFAULT_PROCESSING_STRATEGY, NON_BLOCKING_PROCESSING_STRATEGY);
    }
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {

    return Arrays.asList(new Object[][] {{false},
        // TODO MULE-10618 reenable non-blocking
        // {true}
    });
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/nonblocking-message-processor-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    MuleClient client = muleContext.getClient();
    assertNotNull(client.send("http://localhost:" + proxyPort.getValue() + "/in", "test", null));

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()

        // logger
        .serial(prePost())

        // <response> start
        .serial(pre())

        // logger
        .serial(prePost())

        // request to echo service
        .serial(pre()).serial(prePost()).serial(post())

        // logger
        .serial(prePost())

        // request to echo service
        .serial(pre()).serial(prePost()).serial(post())

        // <response> end
        .serial(prePost()).serial(post());
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
