/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;

import java.util.concurrent.locks.Lock;

import org.springframework.util.StringUtils;

public class DefaultRefreshTokenManager implements MuleContextAware, Initialisable, RefreshTokenManager
{

    private static final int DEFAULT_EXPIRATION = 60 * 1000;

    private ObjectStore<Boolean> refreshedTokens;
    private MuleContext muleContext;

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            ObjectStoreManager osManager = this.muleContext.getRegistry().lookupObject(
                ObjectStoreManager.class);
            this.refreshedTokens = osManager.getObjectStore("RefreshTokenStore", false, 0,
                DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.mule.security.oauth.RefreshTokenManager#refreshToken(org.mule.security.oauth.OAuth2Adapter,
     *      java.lang.String)
     */
    @Override
    public void refreshToken(OAuth2Adapter adapter, String accessTokenId) throws Exception
    {
        if (StringUtils.isEmpty(accessTokenId))
        {
            throw new IllegalArgumentException("Cannot refresh a blank accessTokenId");
        }

        Lock lock = this.muleContext.getLockFactory().createLock(
            String.format("%s:%s", this.getClass().getCanonicalName(), accessTokenId));
        lock.lock();
        try
        {
            if (!this.refreshedTokens.contains(accessTokenId))
            {
                adapter.refreshAccessToken();
                this.refreshedTokens.store(accessTokenId, true);
            }
        }
        finally
        {
            lock.unlock();
        }

    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
