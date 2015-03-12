/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.SecurityProvider;
import org.mule.security.MuleSecurityManager;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;

import org.junit.Test;

public abstract class AuthenticationNamespaceHandlerTestCase extends FunctionalTestCase
{    
    @Test
    public void testSecurityManagerConfigured()
    {
        MuleSecurityManager securityManager = muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER);
        assertNotNull(securityManager);

        Collection<SecurityProvider> providers = securityManager.getProviders();
        assertEquals(2, providers.size());

        assertThat(containsSecurityProvider(providers, UserAndPasswordAuthenticationProvider.class), is(true));
        assertThat(containsSecurityProvider(providers, PreAuthenticatedAuthenticationProvider.class), is(true));
    }

    private boolean containsSecurityProvider(Collection<SecurityProvider> providers, Class authenticationProviderClass)
    {
        for(SecurityProvider provider : providers)
        {
            assertEquals(SpringProviderAdapter.class, provider.getClass());
            if (authenticationProviderClass.equals(((SpringProviderAdapter) provider).getAuthenticationProvider().getClass()))
            {
                return true;
            }
        }
        return false;
    }
}
