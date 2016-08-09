/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.extension.socket.api.connection.tcp.protocol.XmlMessageEOFProtocol;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;

public class XmlMessageEOFProtocolTestCase extends XmlMessageProtocolTestCase {

  @Override
  public void doSetUp() {
    setProtocol(new XmlMessageEOFProtocol());
  }

  @Override
  public void testSlowStream() throws Exception {
    String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

    SlowInputStream bais = new SlowInputStream(msgData.getBytes());

    InputStream result = read(bais);
    assertNotNull(result);
    assertEquals(msgData, IOUtils.toString(result));
  }

}
