/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import static org.junit.Assert.assertSame;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.junit.Test;
import org.mockito.Mockito;

public class ExceptionsTestCase extends AbstractMuleContextTestCase {

  @Test
  public final void testRoutingExceptionNullMessageValidEndpoint() throws Exception {
    OutboundEndpoint endpoint = Mockito.mock(OutboundEndpoint.class);

    RoutingException rex = new RoutingException(getTestEvent(""), endpoint);
    assertSame(endpoint, rex.getRoute());
  }

}
