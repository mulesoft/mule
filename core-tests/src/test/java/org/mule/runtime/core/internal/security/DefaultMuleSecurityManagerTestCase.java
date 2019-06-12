/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static java.util.Optional.ofNullable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMuleSecurityManagerTestCase extends AbstractMuleTestCase {

  @Mock
  private SecurityProvider provider;

  @Mock
  private Authentication authentication;

  @Rule
  public ExpectedException expected = none();

  private SecurityManager manager;

  @Before
  public void setUp() {
    when(provider.getName()).thenReturn("provider");
    when(provider.supports(any())).thenReturn(true);
    manager = new DefaultMuleSecurityManager();
  }

  @Test
  public void authenticateCorrectly()
      throws SecurityException, SecurityProviderNotFoundException {
    when(provider.authenticate(authentication)).thenReturn(authentication);
    manager.addProvider(provider);

    Optional<Authentication> optional = ofNullable(manager.authenticate(authentication));

    verify(provider).authenticate(authentication);

    assertThat(optional.get(), is(authentication));
  }

  @Test
  public void authenticateWithWrongAuthentication()
      throws SecurityException, SecurityProviderNotFoundException {
    when(provider.authenticate(authentication)).thenThrow(UnauthorisedException.class);
    manager.addProvider(provider);

    expected.expect(UnauthorisedException.class);

    try {
      manager.authenticate(authentication);
    } finally {
      verify(provider).authenticate(authentication);
    }
  }

  @Test
  public void authenticateWithFailedProvider()
      throws SecurityException, SecurityProviderNotFoundException {
    when(provider.authenticate(authentication)).thenThrow(SecurityException.class);
    manager.addProvider(provider);

    expected.expect(SecurityException.class);

    try {
      manager.authenticate(authentication);
    } finally {
      verify(provider).authenticate(authentication);
    }
  }

  @Test
  public void authenticateWithoutProvider()
      throws SecurityException, SecurityProviderNotFoundException {
    expected.expect(SecurityProviderNotFoundException.class);

    try {
      manager.authenticate(authentication);
    } finally {
      verifyZeroInteractions(provider);
    }
  }

}
