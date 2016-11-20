/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;

import org.hamcrest.core.Is;
import org.junit.Test;

public class ModuleWithPropertiesTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "module/module-properties/module-properties.xml";
  }

  @Override
  protected String getConfigFile() {
    return "module/flows-using-module-properties.xml";
  }

  @Test
  public void testSetPayloadHardcodedFromModuleFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadHardcodedFromModuleFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("hardcoded value from module"));
  }

  @Test
  public void testSetPayloadParamFromModuleFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamFromModuleFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("new payload from module"));
  }

  @Test
  public void testSetPayloadConfigParamFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadConfigParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("some config-value-parameter"));
  }

  @Test
  public void testSetPayloadConfigDefaultParamFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadConfigDefaultParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("some default-config-value-parameter"));
  }

  @Test
  public void testSetPayloadAddParamAndPropertyValues() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadAddParamAndPropertyValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("a parameter value some config-value-parameter"));
  }
}
