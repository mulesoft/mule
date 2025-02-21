/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Before;
import org.junit.Test;

public class AuthenticationFilterTestCase {

  private TestAuthenticationFilter filter;
  private CoreEvent mockEvent;
  private SecurityContext mockContext;

  @Before
  public void setUp() {
    filter = new TestAuthenticationFilter();
    mockEvent = mock(CoreEvent.class);
    mockContext = mock(SecurityContext.class);
  }

  @Test
  public void testIsAuthenticateDefault() {
    assertThat(filter.isAuthenticate(), is(false));
  }

  @Test
  public void testDoFilterCallsAuthenticate() throws Exception {
    filter.setMockSecurityContext(mockContext);
    SecurityContext result = filter.doFilter(mockEvent);
    assertThat(result, is(mockContext));
  }

  static class TestAuthenticationFilter extends AbstractAuthenticationFilter {

    private SecurityContext mockSecurityContext;

    @Override
    public SecurityContext authenticate(CoreEvent event) {
      return mockSecurityContext;
    }

    public void setMockSecurityContext(SecurityContext mockSecurityContext) {
      this.mockSecurityContext = mockSecurityContext;
    }
  }
}
