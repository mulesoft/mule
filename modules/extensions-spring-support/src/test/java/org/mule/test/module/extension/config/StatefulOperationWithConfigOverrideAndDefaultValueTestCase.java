/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.implicit.exclusive.config.extension.extension.ImplicitConfigWithOptionalParameter.OPTIONAL_PARAMETER_DEFAULT_VALUE;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class StatefulOperationWithConfigOverrideAndDefaultValueTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String OVERRIDEN_VALUE = "Custom value in operation!";
  private static final String CUSTOM_CONFIG_VALUE = "Custom value in config!";
  private static final String DEFALT_VALUE_IN_CONFIG = OPTIONAL_PARAMETER_DEFAULT_VALUE;

  @Override
  protected String getConfigFile() {
    return "stateful-override-with-default-value-config.xml";
  }

  @Test
  public void configValueOverridenByOperation() throws Exception {
    String result = (String) flowRunner("configValueOverriden").run().getMessage().getPayload().getValue();
    assertThat(result, is(OVERRIDEN_VALUE));
  }

  @Test
  public void customConfigValueForParameter() throws Exception {
    String result = (String) flowRunner("customConfigValue").run().getMessage().getPayload().getValue();
    assertThat(result, is(CUSTOM_CONFIG_VALUE));
  }

  @Test
  public void configValueFromImplicitConfig() throws Exception {
    String result = (String) flowRunner("withConfigValueFromImplicitConfig").run().getMessage().getPayload().getValue();
    assertThat(result, is(DEFALT_VALUE_IN_CONFIG));
  }

}
