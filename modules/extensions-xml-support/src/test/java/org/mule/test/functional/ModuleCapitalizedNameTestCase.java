/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;

public class ModuleCapitalizedNameTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-capitalized-name.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-capitalized-name.xml";
  }

  @Test
  public void testSetPayloadHardcoded() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadHardcodedFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testUnderscoreSetPayloadWithParameter() throws Exception {
    final CoreEvent muleEvent = flowRunner("testUnderscoreSetPayloadWithParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("underscore property value, underscore parameter value"));
  }

  @Test
  public void testSetPayloadWithNameParameter() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadWithNameParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("the name parameter"));
  }

}
