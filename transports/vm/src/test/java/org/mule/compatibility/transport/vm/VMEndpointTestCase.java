/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.compatibility.transport.vm.VMConnector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class VMEndpointTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testUrlWithProvider() throws Exception {
    EndpointURI url = new MuleEndpointURI("vm://some.queue?endpointName=vmProvider", muleContext);
    url.initialise();
    assertEquals(VMConnector.VM, url.getScheme());
    assertEquals("some.queue", url.getAddress());
    assertEquals("vmProvider", url.getEndpointName());
    assertEquals("vm://some.queue?endpointName=vmProvider", url.toString());
    assertEquals(1, url.getParams().size());
  }

}
