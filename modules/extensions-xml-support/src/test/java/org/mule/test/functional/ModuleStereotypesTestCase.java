/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Description;
import org.junit.Test;

public class ModuleStereotypesTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-stereotypes.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-stereotypes.xml";
  }

  @Test
  @Description("Runtime test to ensure parameterized stereotypes in global element works")
  public void parameterizedStereotypeDoesCorrectBindingAndConnectorWorks() throws Exception {
    final String data = "some parameterized data";
    final CoreEvent muleEvent = flowRunner("testParameterizedStereotypeDoesCorrectBindingAndConnectorWorks")
        .withVariable("aData", data).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(data));
  }

  @Test
  @Description("Runtime test to ensure parameterized stereotypes in operation works")
  public void testParameterizedStereotypeDoesCorrectBindingAndConnectorWorksForOperation() throws Exception {
    final CoreEvent muleEvent = flowRunner("testParameterizedStereotypeDoesCorrectBindingAndConnectorWorksForOperation").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("a value"));
  }
}
