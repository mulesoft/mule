/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import org.mule.extension.socket.SocketExtensionTestCase;

import org.junit.Test;

public class MixedProtocolTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "mixed-protocols-config.xml";
  }

  @Test
  public void sendString() throws Exception {
    sendString("tcp-send");
  }
}
