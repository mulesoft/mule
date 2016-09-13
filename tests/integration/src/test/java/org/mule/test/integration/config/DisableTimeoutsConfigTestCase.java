/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class DisableTimeoutsConfigTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public SystemProperty disableTimeouts = new SystemProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "timeout.disable", "true");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/config/disable-timeouts-config.xml";
  }

  @Test
  public void httpOutboundEndpointResponseTimeout() throws Exception {
    Event event = flowRunner("HttpTimeout").withPayload("hi").run();
    InternalMessage result = event.getMessage();
    assertNotNull(result);
    assertThat(event.getError().isPresent(), is((false)));
  }

}
