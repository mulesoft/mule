/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyRegistryLookupTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "groovy-registry-lookup-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    muleContext.getRegistry().registerObject("hello", new Hello());
  }

  @Test
  public void testBindingCallout() throws Exception {
    MuleMessage response = flowRunner("sayHello").withPayload("").run().getMessage();
    assertNotNull(response);
    assertEquals("hello", getPayloadAsString(response));
  }

  public static class Hello {

    public String sayHello() {
      return "hello";
    }
  }
}
