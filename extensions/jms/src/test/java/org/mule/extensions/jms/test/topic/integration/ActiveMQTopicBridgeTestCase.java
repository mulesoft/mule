/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.topic.integration;

import static java.util.Arrays.asList;
import org.junit.runners.Parameterized;
import org.mule.test.runner.RunnerDelegateTo;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;


@RunnerDelegateTo(Parameterized.class)
@Features("JMS Extension")
@Stories("ActiveMQ Connection Provider Topic Bridge")
public class ActiveMQTopicBridgeTestCase extends JmsAbstractTopicBridge {

  @Parameterized.Parameter(0)
  public String configName;

  @Parameterized.Parameter(1)
  public String configFileName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"activemq-default", "config/activemq/activemq-default.xml"},
        {"activemq-default-no-caching", "config/activemq/activemq-default-no-caching.xml"},
        {"activemq-default-user-pass", "config/activemq/activemq-default-user-pass.xml"},
        {"activemq-with-overrides", "config/activemq/activemq-with-overrides.xml"}
    });
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {configFileName, BRIDGE_CONFIG_XML};
  }

}
