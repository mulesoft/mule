/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;

public class ModuleWithPropertiesTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-properties.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-properties.xml";
  }

  @Test
  public void testSetPayloadHardcodedFromModuleFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadHardcodedFromModuleFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("hardcoded value from module"));
  }

  @Test
  public void testSetPayloadParamFromModuleFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadParamFromModuleFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("new payload from module"));
  }

  @Test
  public void testSetPayloadConfigParamFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadConfigParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("some config-value-parameter"));
  }

  @Test
  public void testSetPayloadConfigDefaultParamFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadConfigDefaultParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("some default-config-value-parameter"));
  }

  @Test
  public void testSetPayloadConfigDefaultPropertyUseOptionalFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadConfigDefaultPropertyUseOptionalFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("some default-config-value-parameter"));
  }

  @Test
  public void testSetPayloadAddParamAndPropertyValues() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadAddParamAndPropertyValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("a parameter value some config-value-parameter"));
  }

  @Test
  public void testSetPayloadConfigOptionalProperty() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadConfigOptionalProperty").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }
}
