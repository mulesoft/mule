/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.group;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.SomeParameterGroupOneRequiredConfig;

import org.junit.Test;

public class ParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "values/some-parameter-group-config.xml";
  }

  @Test
  public void testShowInDslTrueWithSimpleParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueSomeParameter");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }
  
  @Test
  public void testShowInDslTrueWithComplexParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueComplexParameter");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicSimpleParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueSomeParameterDynamic", "someParameter", "hello dog!");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicComplexParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueComplexParameterDynamic", "anotherParameter", "hello bird!");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testWithSimpleParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("someParameter");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("complexParameter");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testWithDynamicSimpleParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("someParameterDynamic", "someParameter", "hello dog!");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testWithDynamicComplexParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("complexParameterDynamic", "anotherParameter", "hello bird!");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  private SomeParameterGroupOneRequiredConfig runFlowAndGetConfig(String flowName) throws Exception {
    return runFlowAndGetConfig(flowName, "", "");
  }

  private SomeParameterGroupOneRequiredConfig runFlowAndGetConfig(String flowName, String variableName, String variableValue)
      throws Exception {
    return (SomeParameterGroupOneRequiredConfig) flowRunner(flowName).withVariable(variableName, variableValue).run().getMessage()
        .getPayload().getValue();
  }
}
