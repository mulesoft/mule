/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.group;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.SomeAliasedParameterGroupOneRequiredConfig;
import org.mule.test.some.extension.SomeParameterGroupOneRequiredConfig;

import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
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

  @Test
  public void testShowInDslTrueWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueRepeatedNameParameter");
    assertThat(config.getRepeatedNameParameter(), is("hello cat!"));
  }

  @Test
  public void testShowInDslTrueWithComplexParameterWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueComplexParameterWithRepeatedNameParameter");
    assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicSimpleParameterWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueSomeParameterDynamicWithRepeatedNameParameter", "repeatedNameParameter", "hello cat!");
    assertThat(config.getRepeatedNameParameter(), is("hello cat!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicComplexParameterWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueComplexParameterDynamicWithRepeatedNameParameter", "repeatedNameParameter", "hi bird!");
    assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("repeatedNameParameter");
    assertThat(config.getRepeatedNameParameter(), is("hello cat!"));
  }

  @Test
  public void testWithComplexParameterWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config = runFlowAndGetConfig("complexParameterWithRepeatedNameParameter");
    assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testWithDynamicRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("repeatedNameParameterDynamic", "repeatedNameParameter", "hello cat!");
    assertThat(config.getRepeatedNameParameter(), is("hello cat!"));
  }

  @Test
  public void testWithDynamicComplexParameterWithRepeatedNameParameter() throws Exception {
    SomeParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("complexParameterWithRepeatedNameParameterDynamic", "repeatedNameParameter", "hi bird!");
    assertThat(config.getComplexParameter().getRepeatedNameParameter(), is("hi bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInComplexParameter() throws Exception {
    Map<String, String> values = runFlowAndGetValues("dslTrueRepeatedParameterNameInComplexParameter");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hi bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInComplexParameterDynamic() throws Exception {
    Map<String, String> values = runFlowAndGetValues("dslTrueRepeatedParameterNameInComplexParameterDynamic");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hi bird!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInParameterGroup() throws Exception {
    Map<String, String> values = runFlowAndGetValues("dslTrueRepeatedParameterNameInParameterGroup");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hello cat!"));
  }

  @Test
  public void testWithDslTrueRepeatedParameterNameInParameterGroupDynamic() throws Exception {
    Map<String, String> values = runFlowAndGetValues("dslTrueRepeatedParameterNameInParameterGroupDynamic");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hello cat!"));
  }

  @Test
  public void testWithRepeatedParameterNameInComplexParameter() throws Exception {
    Map<String, String> values = runFlowAndGetValues("repeatedParameterNameInComplexParameter");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hi bird!"));
  }

  @Test
  public void testWithRepeatedParameterNameInComplexParameterDynamic() throws Exception {
    Map<String, String> values = runFlowAndGetValues("repeatedParameterNameInComplexParameterDynamic");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hi bird!"));
  }

  @Test
  public void testWithRepeatedParameterNameInParameterGroup() throws Exception {
    Map<String, String> values = runFlowAndGetValues("repeatedParameterNameInParameterGroup");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hello cat!"));;
  }

  @Test
  public void testWithRepeatedParameterNameInParameterGroupDynamic() throws Exception {
    Map<String, String> values = runFlowAndGetValues("repeatedParameterNameInParameterGroupDynamic");
    assertThat(values.get("pojoParameter"), is("hi lizard!"));
    assertThat(values.get("oneParameterGroup"), is("hello cat!"));
  }

  public void testShowInDslTrueWithSimpleParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueSomeParameterAlias");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testShowInDslTrueWithComplexParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config = runFlowAndGetConfig("dslTrueComplexParameterAlias");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicSimpleParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueSomeParameterAliasDynamic", "someParameter", "hello dog!");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testShowInDslTrueWithDynamicComplexParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("dslTrueComplexParameterAliasDynamic", "anotherParameter", "hello bird!");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testWithSimpleParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config = runFlowAndGetConfig("someParameterAlias");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config = runFlowAndGetConfig("complexParameterAlias");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  @Test
  public void testWithDynamicSimpleParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("someParameterAliasDynamic", "someParameter", "hello dog!");
    assertThat(config.getSomeParameter(), is("hello dog!"));
  }

  @Test
  public void testWithDynamicComplexParameterAlias() throws Exception {
    SomeAliasedParameterGroupOneRequiredConfig config =
        runFlowAndGetConfig("complexParameterAliasDynamic", "anotherParameter", "hello bird!");
    assertThat(config.getComplexParameter().getAnotherParameter(), is("hello bird!"));
  }

  private <T> T runFlowAndGetConfig(String flowName) throws Exception {
    return runFlowAndGetConfig(flowName, "", "");
  }

  private <T> T runFlowAndGetConfig(String flowName, String variableName, String variableValue)
      throws Exception {
    return (T) flowRunner(flowName).withVariable(variableName, variableValue).run().getMessage()
        .getPayload().getValue();
  }

  private Map<String, String> runFlowAndGetValues(String flowName) throws Exception {
    return (Map<String, String>) flowRunner(flowName).run().getMessage().getPayload().getValue();
  }
}
