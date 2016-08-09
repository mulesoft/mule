/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.extension.socket.api.connection.tcp.protocol.XmlMessageProtocol;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * Test by reading characters from a fixed StringBuilder instead of a TCP port.
 */
@SmallTest
public class XmlMessageProtocolTestCase extends AbstractMuleTestCase {

  private XmlMessageProtocol xmp;

  protected void setProtocol(XmlMessageProtocol xmp) {
    this.xmp = xmp;
  }

  protected InputStream read(InputStream is) throws IOException {
    return xmp.read(is);
  }

  @Before
  public void doSetUp() {
    setProtocol(new XmlMessageProtocol());
  }

  protected void doTearDown() throws Exception {
    xmp = null;
  }

  @Test
  public void testSingleMessage() throws Exception {
    String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

    ByteArrayInputStream bais = new ByteArrayInputStream(msgData.getBytes());

    InputStream result = read(bais);
    assertNotNull(result);
    assertEquals(msgData, IOUtils.toString(result));


    assertEquals(bais.available(), 0);
  }

  @Test
  public void testTwoMessages() throws Exception {
    String[] msgData = {"<?xml version=\"1.0\"?><data>hello</data>", "<?xml version=\"1.0\"?><data>goodbye</data>"};

    ByteArrayInputStream bais = new ByteArrayInputStream((msgData[0] + msgData[1]).getBytes());

    InputStream result = read(bais);
    assertNotNull(result);
    assertEquals(msgData[0], IOUtils.toString(result));

    result = read(bais);
    assertNotNull(result);
    assertEquals(msgData[1], IOUtils.toString(result));

    assertEquals(bais.available(), 0);
  }

  @Test
  public void testMultipleMessages() throws Exception {
    String[] msgData = {"<?xml version=\"1.0\"?><data>1</data>", "<?xml version=\"1.0\"?><data>22</data>",
        "<?xml version=\"1.0\"?><data>333</data>", "<?xml version=\"1.0\"?><data>4444</data>",
        "<?xml version=\"1.0\"?><data>55555</data>", "<?xml version=\"1.0\"?><data>666666</data>",
        "<?xml version=\"1.0\"?><data>7777777</data>", "<?xml version=\"1.0\"?><data>88888888</data>",
        "<?xml version=\"1.0\"?><data>999999999</data>", "<?xml version=\"1.0\"?><data>aaaaaaaaaa</data>",
        "<?xml version=\"1.0\"?><data>bbbbbbbbbbb</data>", "<?xml version=\"1.0\"?><data>cccccccccccc</data>",
        "<?xml version=\"1.0\"?><data>ddddddddddddd</data>", "<?xml version=\"1.0\"?><data>eeeeeeeeeeeeee</data>",
        "<?xml version=\"1.0\"?><data>fffffffffffffff</data>"};

    StringBuilder allMsgData = new StringBuilder();

    for (int i = 0; i < msgData.length; i++) {
      allMsgData.append(msgData[i]);
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(allMsgData.toString().getBytes());

    InputStream result;

    for (int i = 0; i < msgData.length; i++) {
      result = read(bais);
      assertNotNull(result);
      assertEquals(msgData[i], IOUtils.toString(result));
    }

    assertEquals(bais.available(), 0);
  }

  @Test
  public void testSlowStream() throws Exception {
    String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

    SlowInputStream bais = new SlowInputStream(msgData.getBytes());

    InputStream result = read(bais);
    assertNotNull(result);
    // only get the first character! use XmlMessageEOFProtocol instead
    assertEquals(msgData.substring(0, 1), IOUtils.toString(result));
  }

}
