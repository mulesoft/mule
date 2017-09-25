/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;

public class ModuleContentTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-content.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-content.xml";
  }

  @Test
  public void testSetPayloadUsingContent() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadUsingContent").run();
    final Object value = muleEvent.getMessage().getPayload().getValue();
    assertThat(value, instanceOf(String.class));
    assertThat(value, is("smart connector content"));
  }

  @Test
  public void testSetPayloadUsingContentAndSimpleParameter() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadUsingContentAndSimpleParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("attribute value:[value from attribute], value of content (accessing map under key smart):[smart connector content]"));
  }

  @Test
  public void testSetPayloadUsingPrimary() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadUsingPrimary").run();
    final Object value = muleEvent.getMessage().getPayload().getValue();
    assertThat(value, instanceOf(String.class));
    assertThat(value, is("smart connector primary"));
  }

  @Test
  public void testSetPayloadUsingContentAndPrimaryAndSimpleParameter() throws Exception {
    final CoreEvent muleEvent = flowRunner("testSetPayloadUsingContentAndPrimaryAndSimpleParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("attribute value:[value from attribute], value of content:[smart connector content], value of primary:[smart connector primary]"));
  }
}
