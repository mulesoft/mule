/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.internal.security.DefaultSecurityContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractSecurityProviderTestCase {

  private AbstractSecurityProvider securityProvider;

  @Mock
  private SecurityContextFactory mockSecurityContextFactory;

  @Mock
  private Authentication mockAuthentication;

  private static class TestSecurityProvider extends AbstractSecurityProvider {

    public TestSecurityProvider(String name) {
      super(name);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
      return null;
    }
  }

  @Before
  public void setUp() {
    securityProvider = new TestSecurityProvider("TestProvider");
  }

  @Test
  public void testGetName() {
    assertThat(securityProvider.getName(), is("TestProvider"));
  }

  @Test
  public void testSetName() {
    securityProvider.setName("NewName");
    assertThat(securityProvider.getName(), is("NewName"));
  }

  @Test
  public void testSupportsAuthenticationClass() {
    assertThat(securityProvider.supports(Authentication.class), is(true));
  }

  @Test
  public void testInitialiseWithDefaultSecurityContextFactorySet() throws InitialisationException {
    securityProvider.initialise();
    assertThat(securityProvider.getSecurityContextFactory(), is(instanceOf(DefaultSecurityContextFactory.class)));
  }

  @Test
  public void testSetCustomSecurityContextFactory() {
    securityProvider.setSecurityContextFactory(mockSecurityContextFactory);
    assertThat(securityProvider.getSecurityContextFactory(), is(mockSecurityContextFactory));
  }

  @Test
  public void testCreateSecurityContextWithSecurityContextFactory() throws UnknownAuthenticationTypeException {
    SecurityContext mockSecurityContext = mock(SecurityContext.class);
    when(mockSecurityContextFactory.create(mockAuthentication)).thenReturn(mockSecurityContext);

    securityProvider.setSecurityContextFactory(mockSecurityContextFactory);
    SecurityContext securityContext = securityProvider.createSecurityContext(mockAuthentication);

    assertThat(securityContext, is(mockSecurityContext));
    verify(mockSecurityContextFactory).create(mockAuthentication);
  }
}
