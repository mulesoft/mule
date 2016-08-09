/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

public class StatusCodeMappingsTestCase extends AbstractMuleTestCase {

  MuleContext mockMuleContext = Mockito.mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());

  @Before
  public void setUp() {
    Mockito.when(mockMuleContext.getConfiguration().getId()).thenReturn("1");
    Mockito.when(mockMuleContext.getExecutionClassLoader()).thenReturn(this.getClass().getClassLoader());
  }

  @Test
  public void testErrorMappings() {
    String code = ExceptionHelper.getErrorMapping("http", RoutingException.class, mockMuleContext);
    assertEquals("500", code);

    code = ExceptionHelper.getErrorMapping("HTTP", org.mule.runtime.core.api.security.SecurityException.class, mockMuleContext);
    assertEquals("403", code);

    code = ExceptionHelper.getErrorMapping("http", UnauthorisedException.class, mockMuleContext);
    assertEquals("401", code);

    code = ExceptionHelper.getErrorMapping("blah", DefaultMuleException.class, mockMuleContext);
    assertEquals(String.valueOf(new DefaultMuleException(MessageFactory.createStaticMessage("test")).getExceptionCode()), code);

  }

  @Test
  public void testHttpsErrorMappings() {
    String code = ExceptionHelper.getErrorMapping("httpS", RoutingException.class, mockMuleContext);
    assertEquals("500", code);

    code = ExceptionHelper.getErrorMapping("HTTPS", org.mule.runtime.core.api.security.SecurityException.class, mockMuleContext);
    assertEquals("403", code);

    code = ExceptionHelper.getErrorMapping("https", UnauthorisedException.class, mockMuleContext);
    assertEquals("401", code);
  }
}
