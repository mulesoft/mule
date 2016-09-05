/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CacheControlTestCase extends AbstractMuleTestCase {

  private static final String HEADER_DIRECTIVE = "#[header:directive]";
  private static final String HEADER_MAX_AGE = "#[header:maxAge]";
  private static final String HEADER_MUST_REVALIDATE = "#[header:mustRevalidate]";
  private static final String HEADER_NO_CACHE = "#[header:noCache]";
  private static final String HEADER_NO_STORE = "#[header:noStore]";
  private MuleMessage muleMessage;
  private MuleEvent muleEvent;
  private ExpressionLanguage expressionLanguage;

  @Before
  public void setUp() {
    muleMessage = mock(MuleMessage.class);
    muleEvent = mock(MuleEvent.class);
    expressionLanguage = mock(ExpressionLanguage.class);
  }

  @Test
  public void testCacheControlByDefault() {
    CacheControlHeader cacheControl = new CacheControlHeader();
    cacheControl.parse(muleEvent, expressionLanguage);
    assertEquals("", cacheControl.toString());
  }

  @Test
  public void testCacheControlFullConfig() {
    CacheControlHeader cacheControl = new CacheControlHeader();
    cacheControl.setDirective("public");
    cacheControl.setMaxAge("3600");
    cacheControl.setMustRevalidate("true");
    cacheControl.setNoCache("true");
    cacheControl.setNoStore("true");
    mockParse();
    cacheControl.parse(muleEvent, expressionLanguage);
    assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCacheControlWrongDirective() {
    CacheControlHeader cacheControl = new CacheControlHeader();
    cacheControl.setDirective("anyDirective");
    mockParse();
    cacheControl.parse(muleEvent, expressionLanguage);
  }

  @Test
  public void testCacheControlWithExpressions() {
    CacheControlHeader cacheControl = new CacheControlHeader();
    cacheControl.setDirective(HEADER_DIRECTIVE);
    cacheControl.setMaxAge(HEADER_MAX_AGE);
    cacheControl.setMustRevalidate(HEADER_MUST_REVALIDATE);
    cacheControl.setNoCache(HEADER_NO_CACHE);
    cacheControl.setNoStore(HEADER_NO_STORE);

    when(expressionLanguage.parse(HEADER_DIRECTIVE, muleEvent, null)).thenReturn("public");
    when(expressionLanguage.parse(HEADER_MAX_AGE, muleEvent, null)).thenReturn("3600");
    when(expressionLanguage.parse(HEADER_MUST_REVALIDATE, muleEvent, null)).thenReturn("true");
    when(expressionLanguage.parse(HEADER_NO_CACHE, muleEvent, null)).thenReturn("true");
    when(expressionLanguage.parse(HEADER_NO_STORE, muleEvent, null)).thenReturn("true");

    cacheControl.parse(muleEvent, expressionLanguage);
    assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
  }

  private void mockParse() {
    when(expressionLanguage.parse(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
  }
}
