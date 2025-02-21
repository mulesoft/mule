/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractSecurityFilterTestCase {

  @Mock
  private SecurityManager securityManager;

  @Mock
  private Registry registry;

  @InjectMocks
  private AbstractSecurityFilter securityFilter = spy(new AbstractSecurityFilter() {

    @Override
    public SecurityContext doFilter(CoreEvent event)
        throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException, SecurityProviderNotFoundException,
        EncryptionStrategyNotFoundException, InitialisationException {
      return null;
    }
  });

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testGetSecurityManager() {
    securityFilter.securityManager = securityManager;
    assertThat(securityFilter.getSecurityManager(), is(securityManager));
  }

  @Test
  public void testGetSecurityProvidersInitiallyNull() {
    assertThat(securityFilter.getSecurityProviders(), is(nullValue()));
  }

  @Test(expected = InitialisationException.class)
  public void testInitialiseWithoutSecurityManagerThrowsException() throws InitialisationException {
    securityFilter.securityManager = null;
    when(registry.lookupByName(anyString())).thenReturn(Optional.empty());
    securityFilter.initialise();
  }

  @Test
  public void testInitialise() throws InitialisationException {
    securityFilter.initialise();
    verify(securityFilter, times(1)).doInitialise();
  }
}
