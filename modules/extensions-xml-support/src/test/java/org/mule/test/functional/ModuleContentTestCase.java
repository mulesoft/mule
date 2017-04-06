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
import org.junit.Test;
import org.mule.runtime.core.api.Event;

import java.util.Map;

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
    final Event muleEvent = flowRunner("testSetPayloadUsingContent").run();
    final Object value = muleEvent.getMessage().getPayload().getValue();
    assertThat(value, instanceOf(Map.class));
    assertThat(((Map) value).get("smart"), is("connector"));
  }

  @Test
  public void testSetPayloadUsingContentAndSimpleParameter() throws Exception {
    final Event muleEvent = flowRunner("testSetPayloadUsingContentAndSimpleParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("attribute value:[value from attribute], value of content (accessing map under key smart):[connector]"));
  }

  @Test
  public void testSetPayloadUsingPrimary() throws Exception {
    final Event muleEvent = flowRunner("testSetPayloadUsingPrimary").run();
    final Object value = muleEvent.getMessage().getPayload().getValue();
    assertThat(value, instanceOf(Map.class));
    assertThat(((Map) value).get("smart2"), is("connector2"));
  }

  @Test
  public void testSetPayloadUsingContentAndPrimaryAndSimpleParameter() throws Exception {
    final Event muleEvent = flowRunner("testSetPayloadUsingContentAndPrimaryAndSimpleParameter").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("attribute value:[value from attribute], value of content (accessing map under key smart):[connector], value of primary (accessing map under key smart2):[connector2]"));
  }
}
