/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Test the syntax of the SimpleFilename parser
 */
public class ExpressionFilenameParserTestCase extends AbstractMuleContextEndpointTestCase {

  private ExpressionFilenameParser parser;
  private MuleMessage message;
  private MuleEvent event;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    ExpressionFilenameParser.resetCount();

    parser = new ExpressionFilenameParser();
    parser.setMuleContext(muleContext);

    Map<String, Serializable> inboundProperties = new HashMap<>();
    inboundProperties.put(FileConnector.PROPERTY_ORIGINAL_FILENAME, "originalName");
    inboundProperties.put(FileConnector.PROPERTY_FILENAME, "newName");
    message =
        MuleMessage.builder().payload("hello").inboundProperties(inboundProperties).addOutboundProperty("foo", "bar").build();
    Flow flow = getTestFlow();
    event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build();
  }

  @Test
  public void testWigglyMuleStyleParsing() {
    String result =
        parser.getFilename(event, "Test1_#[org.mule.compatibility.transport.file.ExpressionFilenameParser.count()].txt");
    assertEquals("Test1_0.txt", result);

    result = parser.getFilename(event, "Test2_#[org.mule.runtime.core.util.DateUtils.getTimeStamp('yyMMdd')].txt");
    assertDatestampWithYearMonthAndDayMatches(result);

    result = parser.getFilename(event, "Test3_#[org.mule.runtime.core.util.DateUtils.getTimeStamp('dd-MM-yy_HH-mm-ss.SSS')].txt");
    assertDefaultDatestampMatches(result);

    result = parser.getFilename(event, "Test4_#[server.dateTime.toDate()].txt");
    assertFalse(result.equals("Test4_#[server.dateTime.toDate()].txt"));

    result = parser.getFilename(event, "Test5_#[org.mule.runtime.core.util.UUID.getUUID()].txt");
    assertFalse(result.equals("Test5_#[org.mule.runtime.core.util.UUID.getUUID()].txt"));

    result = parser.getFilename(event, "Test6_#[org.mule.compatibility.transport.file.ExpressionFilenameParser.count()].txt");
    assertEquals("Test6_1.txt", result);

    result = parser.getFilename(event, "Test7_#[message.inboundProperties.originalFilename].txt");
    assertEquals("Test7_originalName.txt", result);

    result = parser.getFilename(event, "Test8_#[message.outboundProperties.foo].txt");
    assertEquals("Test8_bar.txt", result);

    result = parser.getFilename(event, "Test9_#[message.outboundProperties.xxx].txt");
    assertEquals("Test9_null.txt", result);
  }

  @Test
  public void testSquareStyleParsing() {
    String result =
        parser.getFilename(event, "Test1_[org.mule.compatibility.transport.file.ExpressionFilenameParser.count()].txt");
    assertEquals("Test1_0.txt", result);

    result = parser.getFilename(event, "Test2_[org.mule.runtime.core.util.DateUtils.getTimeStamp('yyMMdd')].txt");
    assertDatestampWithYearMonthAndDayMatches(result);

    result = parser.getFilename(event, "Test3_[org.mule.runtime.core.util.DateUtils.getTimeStamp('dd-MM-yy_HH-mm-ss.SSS')].txt");
    assertDefaultDatestampMatches(result);

    result = parser.getFilename(event, "Test4_[server.dateTime.toDate()].txt");
    assertFalse(result.equals("Test4_[server.dateTime.toDate()].txt"));

    result = parser.getFilename(event, "Test5_[org.mule.runtime.core.util.UUID.getUUID()].txt");
    assertFalse(result.equals("Test5_[org.mule.runtime.core.util.UUID.getUUID()].txt"));

    result = parser.getFilename(event, "Test6_[org.mule.compatibility.transport.file.ExpressionFilenameParser.count()].txt");
    assertEquals("Test6_1.txt", result);

    result = parser.getFilename(event, "Test7_[message.inboundProperties.originalFilename].txt");
    assertEquals("Test7_originalName.txt", result);

    result = parser.getFilename(event, "Test8_[message.outboundProperties.foo].txt");
    assertEquals("Test8_bar.txt", result);

    result = parser.getFilename(event, "Test9_[message.outboundProperties.xxx].txt");
    assertEquals("Test9_null.txt", result);
  }

  private void assertDatestampWithYearMonthAndDayMatches(String result) {
    Date now = new Date();
    String expected = String.format("Test2_%1$ty%1$tm%1$td.txt", now);
    assertEquals(expected, result);
  }

  private void assertDefaultDatestampMatches(String result) {
    Date now = new Date();

    // can't compare exactly as the time differs between formatting the expected
    // result and the actual invocation of the function
    String expected = String.format("Test3_%1$td-%1$tm-%1$ty_%1$tH-%1$tM-.*.txt", now);

    assertTrue(result.matches(expected));
  }
}
