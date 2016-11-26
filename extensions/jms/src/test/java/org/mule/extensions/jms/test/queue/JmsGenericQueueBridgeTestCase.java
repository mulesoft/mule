/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.queue;

import static java.util.Arrays.asList;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;


@RunnerDelegateTo(Parameterized.class)
@Features("JMS Extension")
@Stories("Generic Connection Provider Queue Bridge")
public class JmsGenericQueueBridgeTestCase extends JmsAbstractQueueBridge {

  @Parameterized.Parameter(0)
  public String configName;

  @Parameterized.Parameter(1)
  public String configFileName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // TODO MULE-10962: migrate jndi-destinations-always.xml with custom JndiDestinationResolver
        {"jndi-destinations-never", "config/generic/jndi-destinations-never.xml"},
        {"jndi-destinations-try", "config/generic/jndi-destinations-try.xml"}
    });
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {configFileName, BRIDGE_CONFIG_XML};
  }

}
