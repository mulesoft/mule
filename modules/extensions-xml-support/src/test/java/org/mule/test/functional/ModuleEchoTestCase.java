/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;


import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class ModuleEchoTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-echo.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-echo.xml";
  }

  @Test
  public void verifyDataType() throws Exception {
    CoreEvent muleEvent = flowRunner("test").withMediaType(MediaType.JSON).withPayload("{ \"name\":\"Emiliano\" }").run();
    MatcherAssert.assertThat(muleEvent.getMessage().getPayload().getValue(), is(MediaType.JSON.toRfcString()));
  }

}
