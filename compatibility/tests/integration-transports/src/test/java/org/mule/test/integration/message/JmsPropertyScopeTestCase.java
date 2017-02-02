/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.functional.extensions.CompatibilityFunctionalTestCaseRunnerConfig;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.ThreadingProfileConfigurationBuilder;

import java.util.List;

import org.junit.Test;

public class JmsPropertyScopeTestCase extends AbstractPropertyScopeTestCase implements
    CompatibilityFunctionalTestCaseRunnerConfig {

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new ThreadingProfileConfigurationBuilder());
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/jms-property-scope-flow.xml";
  }

  @Override
  @Test
  public void testRequestResponse() throws Exception {
    MuleClient client = muleContext.getClient();

    final InternalMessage message = InternalMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty("foo", "fooValue")
        .addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://reply").build();

    client.dispatch("inbound", message);
    InternalMessage result = client.request("jms://reply", 10000).getRight().get();

    assertNotNull(result);
    assertEquals("test bar", result.getPayload().getValue());
    assertEquals("fooValue", result.getInboundProperty("foo"));
  }
}
