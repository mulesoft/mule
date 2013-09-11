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
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.config.i18n.MessageFactory;

import java.util.concurrent.locks.Lock;

import org.springframework.util.StringUtils;

/**
 * Implementation of {@link RefreshTokenManager} that guarantees that no refresh
 * token is used more than once. If two threads try to refresh the same token
 * concurrently, only one will succeed and the other one will rely on the result of
 * the first one
 */
public class DefaultRefreshTokenManager implements MuleContextAware, RefreshTokenManager
{

    private static final int DEFAULT_EXPIRATION = 60 * 1000;

    private ObjectStore<Boolean> refreshedTokens;
    private MuleContext muleContext;

    /**
     * {@inheritDoc} This implementation uses a lock to guarantee that no refresh
     * token is consumed more than once
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
            if (!this.getRefreshedTokens().contains(accessTokenId))
            {
                adapter.refreshAccessToken(accessTokenId);
                this.getRefreshedTokens().store(accessTokenId, true);
            }
        }
        finally
        {
            lock.unlock();
        }

    }

    private ObjectStore<Boolean> getRefreshedTokens()
    {
        if (this.refreshedTokens == null)
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
                throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Could not obtain ObjectStoreManager"), e);
            }
        }
        return this.refreshedTokens;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
