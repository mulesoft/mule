/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.spring.security.filter.http;


import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.service.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import org.mule.compatibility.module.http.internal.filter.HttpBasicAuthenticationFilter;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.module.http.internal.filter.BasicUnauthorisedException;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class HttpBasicAuthenticationFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testAuthenticationHeaderFailure() throws Exception {
    Event oldEvent = getCurrentEvent();

    final HttpRequestAttributes attrs = mock(HttpRequestAttributes.class);
    when(attrs.getHeaders()).thenReturn(new ParameterMap(singletonMap(AUTHORIZATION, "Basic a")));

    Event event = eventBuilder().message(InternalMessage.builder().payload("a").attributes(attrs).build()).build();
    setCurrentEvent(event);

    HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();

    SecurityManager manager = mock(SecurityManager.class);
    filter.setSecurityManager(manager);

    doThrow(new UnauthorisedException(mock(I18nMessage.class))).when(manager).authenticate(anyObject());

    try {
      filter.authenticate(event);
      fail("An UnauthorisedException should be thrown");
    } catch (BasicUnauthorisedException e) {
      assertThat(e.getErrorMessage().getAttributes(), instanceOf(HttpResponseAttributes.class));
      HttpResponseAttributes attributes = (HttpResponseAttributes) e.getErrorMessage().getAttributes();
      assertThat(attributes.getHeaders().get(WWW_AUTHENTICATE), is("Basic realm="));
      verify(manager);
    }
    setCurrentEvent(oldEvent);
  }
}
