/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import static org.mule.api.store.ObjectStoreManager.UNBOUNDED;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.util.store.ProvidedObjectStoreWrapper;

import java.util.concurrent.locks.Lock;

import org.apache.commons.collections.Factory;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation of {@link RefreshTokenManager} that guarantees that no refresh
 * token is used more than once. If two threads try to refresh the same token
 * concurrently, only one will succeed and the other one will rely on the result of
 * the first one
 */
public class DefaultRefreshTokenManager implements MuleContextAware, RefreshTokenManager
{

    private volatile ObjectStore<Boolean> refreshedTokens;
    private MuleContext muleContext;
    private int minRefreshInterval = DEFAULT_MIN_REFRESH_INTERVAL;

    /**
     * {@inheritDoc} This implementation uses a lock to guarantee that no refresh
     * token is consumed more than once
     *
     * @see org.mule.security.oauth.RefreshTokenManager#refreshToken(org.mule.security.oauth.OAuth2Adapter,
     * java.lang.String)
     */
    @Override
    public void refreshToken(OAuth2Adapter adapter, String accessTokenId) throws Exception
    {
        if (StringUtils.isEmpty(accessTokenId))
        {
            throw new IllegalArgumentException("Cannot refresh a blank accessTokenId");
        }

        String id = String.format("%s:%s:%s", this.getClass().getCanonicalName(), adapter.getName(), accessTokenId);
        Lock lock = this.muleContext.getLockFactory().createLock(id);
        lock.lock();
        try
        {
            if (!this.getRefreshedTokens().contains(id))
            {
                adapter.refreshAccessToken(accessTokenId);
                this.getRefreshedTokens().store(id, true);
            }
        }
        finally
        {
            lock.unlock();
        }

    }

    private synchronized ObjectStore<Boolean> getRefreshedTokens()
    {
        if (this.refreshedTokens == null)
        {
            this.refreshedTokens = new ProvidedObjectStoreWrapper<>(null, internalObjectStoreFactory());
        }
        return this.refreshedTokens;
    }

    protected Factory internalObjectStoreFactory()
    {
        return new Factory()
        {
            @Override
            public Object create()
            {
                ObjectStoreManager osManager = muleContext.getObjectStoreManager();
                return osManager.getObjectStore("RefreshTokenStore", false, UNBOUNDED,
                        minRefreshInterval, minRefreshInterval);
            }
        };
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void setMinRefreshIntervalInMillis(int minRefreshIntervalInMillis)
    {
        this.minRefreshInterval = minRefreshIntervalInMillis;
    }

    @Override
    public synchronized void setRefreshedTokensStore(ObjectStore<Boolean> refreshedTokens) throws IllegalStateException
    {
        if (this.refreshedTokens == null)
        {
            this.refreshedTokens = new ProvidedObjectStoreWrapper<>(refreshedTokens, internalObjectStoreFactory());
        }
        else
        {
            throw new IllegalStateException("refreshedTokens object store had already been set/obtained for this DefaultRefreshTokenManager: " + toString());
        }
    }
}
