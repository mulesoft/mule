/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class CookieWrapperTestCase extends AbstractMuleTestCase
{
    private CookieWrapper cookieWrapper;
    private ExpressionManager mockExpressionManager;
    private MuleMessage mockMuleMessage;

    @Before
    public void setUp()
    {
        cookieWrapper = new CookieWrapper();
        mockExpressionManager = mock(ExpressionManager.class);
        mockMuleMessage = mock(MuleMessage.class);
    }

    @Test
    public void testCookieWrapper() throws ParseException
    {
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setDomain("localhost");
        cookieWrapper.setPath("/");
        cookieWrapper.setMaxAge("3600");
        cookieWrapper.setSecure("true");
        cookieWrapper.setVersion("1");

        mockParse();

        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();

        assertEquals("test", cookie.getName());
        assertEquals("test", cookie.getValue());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.getSecure());
        assertEquals(1, cookie.getVersion());
    }

    @Test
    public void testCookieWrapperWithExpressions() throws ParseException
    {
        cookieWrapper.setName("#[name]");
        cookieWrapper.setValue("#[value]");
        cookieWrapper.setDomain("#[domain]");
        cookieWrapper.setPath("#[path]");
        cookieWrapper.setMaxAge("#[maxAge]");
        cookieWrapper.setSecure("#[secure]");
        cookieWrapper.setVersion("#[version]");

        when(mockExpressionManager.parse("#[name]", mockMuleMessage)).thenReturn("test");
        when(mockExpressionManager.parse("#[value]", mockMuleMessage)).thenReturn("test");
        when(mockExpressionManager.parse("#[domain]", mockMuleMessage)).thenReturn("localhost");
        when(mockExpressionManager.parse("#[path]", mockMuleMessage)).thenReturn("/");
        when(mockExpressionManager.parse("#[maxAge]", mockMuleMessage)).thenReturn("3600");
        when(mockExpressionManager.parse("#[secure]", mockMuleMessage)).thenReturn("true");
        when(mockExpressionManager.parse("#[version]", mockMuleMessage)).thenReturn("1");

        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();

        assertEquals("test", cookie.getName());
        assertEquals("test", cookie.getValue());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.getSecure());
        assertEquals(1, cookie.getVersion());
    }

    @Test
    public void testCookieWrapperOnlyRequiredAttributes() throws ParseException
    {
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");

        mockParse();

        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();

        assertEquals("test=test", cookie.toString());
    }

    @Test
    public void testCookieWrapperStringExpiryDate() throws ParseException
    {
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate("Sun, 15 Dec 2013 16:00:00 GMT");

        mockParse();
        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();
        Date expiryDate = cookie.getExpiryDate();

        SimpleDateFormat formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        assertNotNull("Sun, 15 Dec 2013 16:00:00 GMT", formatter.format(expiryDate));
    }

    @Test
    public void testCookieWrapperExpiryDate() throws ParseException
    {
        Date now = new Date();
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate(now);

        mockParse();
        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();

        Date expiryDate = cookie.getExpiryDate();
        assertEquals(0, now.compareTo(expiryDate));
    }

    @Test
    public void testCookieWrapperExpiryDateExpression() throws ParseException
    {
        Date now = new Date();
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate("#[expiryDate]");

        when(mockExpressionManager.isExpression("#[expiryDate]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[expiryDate]", mockMuleMessage)).thenReturn(now);
        mockParse();

        cookieWrapper.parse(mockMuleMessage, mockExpressionManager);
        Cookie cookie = cookieWrapper.createCookie();

        Date expiryDate = cookie.getExpiryDate();
        assertEquals(0, now.compareTo(expiryDate));
    }


    private void mockParse()
    {
         when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
             new Answer<Object>()
             {
                 @Override
                 public Object answer(InvocationOnMock invocation) throws Throwable
                 {
                     return invocation.getArguments()[0];
                 }
             }
         );
    }



}
