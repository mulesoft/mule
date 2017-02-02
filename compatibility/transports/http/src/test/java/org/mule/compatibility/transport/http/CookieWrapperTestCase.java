/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.fromObject;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CookieWrapperTestCase extends AbstractMuleTestCase {

  private CookieWrapper cookieWrapper;
  private ExtendedExpressionManager mockExpressionManager;
  private Event mockMuleEvent;

  @Before
  public void setUp() {
    cookieWrapper = new CookieWrapper();
    mockExpressionManager = mock(ExtendedExpressionManager.class);
    mockMuleEvent = mock(Event.class);
  }

  @Test
  public void testCookieWrapper() throws ParseException {
    cookieWrapper.setName("test");
    cookieWrapper.setValue("test");
    cookieWrapper.setDomain("localhost");
    cookieWrapper.setPath("/");
    cookieWrapper.setMaxAge("3600");
    cookieWrapper.setSecure("true");
    cookieWrapper.setVersion("1");

    mockParse();

    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();

    assertEquals("test", cookie.getName());
    assertEquals("test", cookie.getValue());
    assertEquals("localhost", cookie.getDomain());
    assertEquals("/", cookie.getPath());
    assertTrue(cookie.getSecure());
    assertEquals(1, cookie.getVersion());
  }

  @Test
  public void testCookieWrapperWithExpressions() throws ParseException {
    cookieWrapper.setName("#[mel:name]");
    cookieWrapper.setValue("#[mel:value]");
    cookieWrapper.setDomain("#[mel:domain]");
    cookieWrapper.setPath("#[mel:path]");
    cookieWrapper.setMaxAge("#[mel:maxAge]");
    cookieWrapper.setSecure("#[mel:secure]");
    cookieWrapper.setVersion("#[mel:version]");

    when(mockExpressionManager.parse("#[mel:name]", mockMuleEvent, null)).thenReturn("test");
    when(mockExpressionManager.parse("#[mel:value]", mockMuleEvent, null)).thenReturn("test");
    when(mockExpressionManager.parse("#[mel:domain]", mockMuleEvent, null)).thenReturn("localhost");
    when(mockExpressionManager.parse("#[mel:path]", mockMuleEvent, null)).thenReturn("/");
    when(mockExpressionManager.parse("#[mel:maxAge]", mockMuleEvent, null)).thenReturn("3600");
    when(mockExpressionManager.parse("#[mel:secure]", mockMuleEvent, null)).thenReturn("true");
    when(mockExpressionManager.parse("#[mel:version]", mockMuleEvent, null)).thenReturn("1");

    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();

    assertEquals("test", cookie.getName());
    assertEquals("test", cookie.getValue());
    assertEquals("localhost", cookie.getDomain());
    assertEquals("/", cookie.getPath());
    assertTrue(cookie.getSecure());
    assertEquals(1, cookie.getVersion());
  }

  @Test
  public void testCookieWrapperOnlyRequiredAttributes() throws ParseException {
    cookieWrapper.setName("test");
    cookieWrapper.setValue("test");

    mockParse();

    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();

    assertEquals("test=test", cookie.toString());
  }

  @Test
  public void testCookieWrapperStringExpiryDate() throws ParseException {
    cookieWrapper.setName("test");
    cookieWrapper.setValue("test");
    cookieWrapper.setExpiryDate("Sun, 15 Dec 2013 16:00:00 GMT");

    mockParse();
    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();
    Date expiryDate = cookie.getExpiryDate();

    SimpleDateFormat formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

    assertNotNull("Sun, 15 Dec 2013 16:00:00 GMT", formatter.format(expiryDate));
  }

  @Test
  public void testCookieWrapperExpiryDate() throws ParseException {
    Date now = new Date();
    cookieWrapper.setName("test");
    cookieWrapper.setValue("test");
    cookieWrapper.setExpiryDate(now);

    mockParse();
    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();

    Date expiryDate = cookie.getExpiryDate();
    assertEquals(0, now.compareTo(expiryDate));
  }

  @Test
  public void testCookieWrapperExpiryDateExpression() throws ParseException {
    Date now = new Date();
    cookieWrapper.setName("test");
    cookieWrapper.setValue("test");
    cookieWrapper.setExpiryDate("#[mel:expiryDate]");

    when(mockExpressionManager.isExpression("#[mel:expiryDate]")).thenReturn(true);
    TypedValue typedValue = new TypedValue<>(now, fromObject(now));
    when(mockExpressionManager.evaluate("#[mel:expiryDate]", mockMuleEvent)).thenReturn(typedValue);
    mockParse();

    cookieWrapper.parse(mockMuleEvent, mockExpressionManager);
    Cookie cookie = cookieWrapper.createCookie();

    Date expiryDate = cookie.getExpiryDate();
    assertEquals(0, now.compareTo(expiryDate));
  }


  private void mockParse() {
    when(mockExpressionManager.parse(anyString(), any(Event.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
  }



}
