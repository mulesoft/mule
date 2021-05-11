/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.ParameterGroupConfiguration;

import org.junit.Test;

public class ConfigWithParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "values/some-config-parameter-group.xml";
  }

  @Test
  public void test() throws Exception {
    ParameterGroupConfiguration config = runFlowAndGetConfig("getConfig", "", "");
    assertThat(config.getRepeatedNameParameter(), is("configParameter"));
    assertThat(config.getComplexParameter().getAnotherParameter(), is("complexParameter"));
    assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("complexParameter"));
  }

  private ParameterGroupConfiguration runFlowAndGetConfig(String flowName, String variableName, String variableValue)
      throws Exception {
    return (ParameterGroupConfiguration) flowRunner(flowName).withVariable(variableName, variableValue).run().getMessage()
        .getPayload().getValue();
  }
}
