/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.ParameterGroupConfig;
import org.mule.test.some.extension.ParameterGroupDslConfig;

import org.junit.Test;

public class ConfigWithParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "values/some-config-parameter-group.xml";
  }

  @Test
  public void parameterGroupConfigRepeatedNameParameter() throws Exception {
    ParameterGroupConfig config = getPayloadValue("flowParameterGroupConfigRepeatedNameParameter");
    assertThat(config.getSomeParameterGroup().getRepeatedNameParameter(), is("inline"));
  }

  @Test
  public void parameterGroupConfigComplexParameter() throws Exception {
    ParameterGroupConfig config = getPayloadValue("flowParameterGroupConfigComplexParameter");
    assertThat(config.getSomeParameterGroup().getComplexParameter().getRepeatedNameParameter(), is("complexParameter"));
  }

  @Test
  public void parameterGroupConfigRepeatedNameParameterDynamic() throws Exception {
    ParameterGroupConfig config =
        getPayloadValue("flowParameterGroupConfigRepeatedNameParameterDynamic", "repeatedNameParameter", "inlineDynamic");
    assertThat(config.getSomeParameterGroup().getRepeatedNameParameter(), is("inlineDynamic"));
  }

  @Test
  public void parameterGroupConfigComplexParameterDynamic() throws Exception {
    ParameterGroupConfig config =
        getPayloadValue("flowParameterGroupConfigComplexParameterDynamic", "repeatedNameParameter", "complexParameterDynamic");
    assertThat(config.getSomeParameterGroup().getComplexParameter().getRepeatedNameParameter(), is("complexParameterDynamic"));
  }

  @Test
  public void parameterGroupDslConfigRepeatedNameParameter() throws Exception {
    ParameterGroupDslConfig config = getPayloadValue("flowParameterGroupDslConfigRepeatedNameParameter");
    assertThat(config.getSomeParameterGroup().getRepeatedNameParameter(), is("dsl"));
  }

  @Test
  public void parameterGroupDslConfigComplexParameter() throws Exception {
    ParameterGroupDslConfig config = getPayloadValue("flowParameterGroupDslConfigComplexParameter");
    assertThat(config.getSomeParameterGroup().getComplexParameter().getRepeatedNameParameter(), is("complexParameterDsl"));
  }

  @Test
  public void parameterGroupDslConfigRepeatedNameParameterDynamic() throws Exception {
    ParameterGroupDslConfig config =
        getPayloadValue("flowParameterGroupDslConfigRepeatedNameParameterDynamic", "repeatedNameParameter", "dslDynamic");
    assertThat(config.getSomeParameterGroup().getRepeatedNameParameter(), is("dslDynamic"));
  }

  @Test
  public void parameterGroupDslConfigComplexParameterDynamic() throws Exception {
    ParameterGroupDslConfig config = getPayloadValue("flowParameterGroupDslConfigComplexParameterDynamic",
                                                     "repeatedNameParameter", "complexParameterDslDynamic");
    assertThat(config.getSomeParameterGroup().getComplexParameter().getRepeatedNameParameter(), is("complexParameterDslDynamic"));
  }

  private <T> T getPayloadValue(String flowName) throws Exception {
    return getPayloadValue(flowName, "", "");
  }

  private <T> T getPayloadValue(String flowName, String variableName, String variableValue)
      throws Exception {
    return (T) flowRunner(flowName).withVariable(variableName, variableValue).run().getMessage().getPayload().getValue();
  }
}
