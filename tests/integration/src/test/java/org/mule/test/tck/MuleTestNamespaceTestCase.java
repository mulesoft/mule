/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.CounterCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MuleTestNamespaceTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "test-namespace-config-flow.xml";
  }

  @Test
  public void testComponent1Config() throws Exception {
    Object object = getComponent("testService1");
    assertNotNull(object);
    assertTrue(object instanceof FunctionalTestComponent);
    FunctionalTestComponent ftc = (FunctionalTestComponent) object;

    assertFalse(ftc.isEnableMessageHistory());
    assertFalse(ftc.isEnableNotifications());
    assertNull(ftc.getAppendString());
    assertEquals("Foo Bar Car Jar", ftc.getReturnData());
    assertNotNull(ftc.getEventCallback());
    assertTrue(ftc.getEventCallback() instanceof CounterCallback);
  }

  @Test
  public void testComponent3Config() throws Exception {
    Object object = getComponent("testService3");
    assertNotNull(object);
    assertTrue(object instanceof FunctionalTestComponent);
    FunctionalTestComponent ftc = (FunctionalTestComponent) object;

    assertFalse(ftc.isEnableMessageHistory());
    assertTrue(ftc.isEnableNotifications());
    assertEquals(" #[mel:context:serviceName]", ftc.getAppendString());
    assertNull(ftc.getReturnData());
    assertNull(ftc.getEventCallback());
  }
}
