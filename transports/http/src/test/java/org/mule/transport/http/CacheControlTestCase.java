/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class CacheControlTestCase extends AbstractMuleTestCase
{

    private static final String HEADER_DIRECTIVE = "#[header:directive]";
    private static final String HEADER_MAX_AGE = "#[header:maxAge]";
    private static final String HEADER_MUST_REVALIDATE = "#[header:mustRevalidate]";
    private static final String HEADER_NO_CACHE = "#[header:noCache]";
    private static final String HEADER_NO_STORE = "#[header:noStore]";
    private MuleMessage muleMessage;
    private ExpressionManager expressionManager;

    @Before
    public void setUp()
    {
        muleMessage = mock(MuleMessage.class);
        expressionManager = mock(ExpressionManager.class);
    }

    @Test
    public void testCacheControlByDefault()
    {
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.parse(muleMessage, expressionManager);
        assertEquals("", cacheControl.toString());
    }

    @Test
    public void testCacheControlFullConfig()
    {
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("public");
        cacheControl.setMaxAge("3600");
        cacheControl.setMustRevalidate("true");
        cacheControl.setNoCache("true");
        cacheControl.setNoStore("true");
        mockParse();
        cacheControl.parse(muleMessage, expressionManager);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheControlWrongDirective()
    {
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("anyDirective");
        mockParse();
        cacheControl.parse(muleMessage, expressionManager);
    }

    @Test
    public void testCacheControlWithExpressions()
    {
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective(HEADER_DIRECTIVE);
        cacheControl.setMaxAge(HEADER_MAX_AGE);
        cacheControl.setMustRevalidate(HEADER_MUST_REVALIDATE);
        cacheControl.setNoCache(HEADER_NO_CACHE);
        cacheControl.setNoStore(HEADER_NO_STORE);

        when(expressionManager.parse(HEADER_DIRECTIVE, muleMessage)).thenReturn("public");
        when(expressionManager.parse(HEADER_MAX_AGE, muleMessage)).thenReturn("3600");
        when(expressionManager.parse(HEADER_MUST_REVALIDATE, muleMessage)).thenReturn("true");
        when(expressionManager.parse(HEADER_NO_CACHE, muleMessage)).thenReturn("true");
        when(expressionManager.parse(HEADER_NO_STORE, muleMessage)).thenReturn("true");

        cacheControl.parse(muleMessage, expressionManager);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
    }

    private void mockParse()
    {
         when(expressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
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
