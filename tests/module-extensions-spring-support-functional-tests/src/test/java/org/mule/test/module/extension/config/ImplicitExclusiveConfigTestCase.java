/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ImplicitExclusiveConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Parameterized.Parameter(value = 0)
  public String configName;

  @Parameterized.Parameter(value = 1)
  public int parameterValue;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"implicit-exclusive-config.xml", 10}, {"multiple-implicit-exclusive-config.xml", 5},
        {"implicit-exclusive-config-with-declared-configs.xml", 5}});
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

  @Test
  public void getImplicitConfig() throws Exception {
    Integer value = (Integer) flowRunner("implicitConfig").run().getMessage().getPayload().getValue();
    assertThat(value, is(parameterValue));
  }
}
