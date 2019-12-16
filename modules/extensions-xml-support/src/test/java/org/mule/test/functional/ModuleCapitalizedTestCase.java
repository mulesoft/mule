/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;

import org.hamcrest.core.Is;
import org.junit.Test;

public class ModuleCapitalizedTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-capitalized.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-capitalized.xml";
  }

  @Test
  public void test_setPayloadHardcodedValueCamelized() throws Exception {
    CoreEvent event = flowRunner("test_setPayloadHardcodedValueCamelized").run();
    assertThat(event.getMessage().getPayload().getValue(), Is.is("hardcoded value"));
  }

  @Test
  public void test_OperationStartedWithCapitalizedNameFlow() throws Exception {
    CoreEvent event = flowRunner("test_OperationStartedWithCapitalizedNameFlow").run();
    assertThat(event.getMessage().getPayload().getValue(), Is.is("hardcoded value"));
  }


}
