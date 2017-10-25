/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthenticationHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private SecurityContext securityContext;

  @Mock
  private SecurityManager manager;

  @Mock
  private Consumer<SecurityContext> securityContextUpdater;

  @Mock
  private Authentication authentication;

  private AuthenticationHandler authenticationHandler;

  @Before
  public void setUp() throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {
    when(manager.authenticate(authentication)).thenReturn(authentication);
    when(manager.createSecurityContext(authentication)).thenReturn(securityContext);
  }

  @Test
  public void setAuthentication()
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    authenticationHandler = new DefaultAuthenticationHandler(securityContext, manager, securityContextUpdater);

    authenticationHandler.setAuthentication(authentication);

    verify(manager).authenticate(authentication);
    verify(securityContext).setAuthentication(authentication);
    verify(securityContextUpdater).accept(securityContext);
    verifyNoMoreInteractions(manager, securityContext, securityContextUpdater);
  }

  @Test
  public void setAuthenticationNullContext()
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    authenticationHandler = new DefaultAuthenticationHandler(null, manager, securityContextUpdater);

    authenticationHandler.setAuthentication(authentication);

    verify(manager).authenticate(authentication);
    verify(manager).createSecurityContext(authentication);
    verify(securityContext).setAuthentication(authentication);
    verify(securityContextUpdater).accept(securityContext);
    verifyNoMoreInteractions(manager, securityContext, securityContextUpdater);
  }

  @Test
  public void getAuthentication()
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    authenticationHandler = new DefaultAuthenticationHandler(securityContext, manager, securityContextUpdater);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    Optional<Authentication> optional = authenticationHandler.getAuthentication();

    assertThat(optional.isPresent(), is(true));
    assertThat(optional.get(), is(authentication));
  }

  @Test
  public void getAuthenticationWhenContextHasNullAuthentication()
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    authenticationHandler = new DefaultAuthenticationHandler(securityContext, manager, securityContextUpdater);
    when(securityContext.getAuthentication()).thenReturn(null);

    Optional<Authentication> optional = authenticationHandler.getAuthentication();

    assertThat(optional.isPresent(), is(false));
  }

  @Test
  public void getAuthenticationWhenNullContext()
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    authenticationHandler = new DefaultAuthenticationHandler(null, manager, securityContextUpdater);

    Optional<Authentication> optional = authenticationHandler.getAuthentication();

    assertThat(optional.isPresent(), is(false));
  }

}
