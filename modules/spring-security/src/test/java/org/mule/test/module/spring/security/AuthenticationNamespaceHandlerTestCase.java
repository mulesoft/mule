/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.spring.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.security.DefaultMuleSecurityManager;
import org.mule.runtime.module.spring.security.PreAuthenticatedAuthenticationProvider;
import org.mule.runtime.module.spring.security.SpringProviderAdapter;
import org.mule.runtime.module.spring.security.UserAndPasswordAuthenticationProvider;

import java.util.Collection;

import org.junit.Test;

public abstract class AuthenticationNamespaceHandlerTestCase extends MuleArtifactFunctionalTestCase {

  @Test
  public void testSecurityManagerConfigured() {
    DefaultMuleSecurityManager securityManager = muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
    assertNotNull(securityManager);

    Collection<SecurityProvider> providers = securityManager.getProviders();
    assertEquals(2, providers.size());

    assertThat(containsSecurityProvider(providers, UserAndPasswordAuthenticationProvider.class), is(true));
    assertThat(containsSecurityProvider(providers, PreAuthenticatedAuthenticationProvider.class), is(true));
  }

  private boolean containsSecurityProvider(Collection<SecurityProvider> providers, Class authenticationProviderClass) {
    for (SecurityProvider provider : providers) {
      assertEquals(SpringProviderAdapter.class, provider.getClass());
      if (authenticationProviderClass.equals(((SpringProviderAdapter) provider).getAuthenticationProvider().getClass())) {
        return true;
      }
    }
    return false;
  }
}
