/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.issues;

import static org.junit.Assert.assertTrue;

import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.compatibility.transport.tcp.TcpFunctionalTestCase;

import org.junit.Test;

/**
 * This is just to check that the Boolean (rather than boolean) doesn't cause any problems
 */
public class ReuseMule2069TestCase extends TcpFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "reuse-mule-2069-flow.xml";
  }

  @Test
  public void testReuseSetOnConnector() {
    assertTrue(((TcpConnector) muleContext.getRegistry().lookupObject(TcpConnector.TCP)).isReuseAddress().booleanValue());
  }

}
