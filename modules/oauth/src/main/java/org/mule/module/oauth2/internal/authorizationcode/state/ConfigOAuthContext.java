/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.state;

import org.mule.api.store.ListableObjectStore;
import org.mule.util.lock.LockFactory;
import org.mule.util.store.ObjectStoreToMapAdapter;

import java.util.concurrent.locks.Lock;

/**
 * Provides the OAuth context for a particular config
 */
public class ConfigOAuthContext
{

    private final LockFactory lockFactory;
    private final String configName;
    private final ObjectStoreToMapAdapter<ResourceOwnerOAuthContext> oauthContextStore;

    public ConfigOAuthContext(final LockFactory lockFactory, ListableObjectStore<ResourceOwnerOAuthContext> objectStore, final String configName)
    {
        this.lockFactory = lockFactory;
        this.oauthContextStore = new ObjectStoreToMapAdapter(objectStore);
        this.configName = configName;
    }

    /**
     * Retrieves the oauth context for a particular user. If there's no state for that user a new state is retrieve so never returns null.
     *
     * @param resourceOwnerId id of the user.
     * @return oauth state
     */
    public ResourceOwnerOAuthContext getContextForResourceOwner(final String resourceOwnerId)
    {
        ResourceOwnerOAuthContext resourceOwnerOAuthContext = null;
        if (!oauthContextStore.containsKey(resourceOwnerId))
        {
            final Lock lock = lockFactory.createLock(configName + "-config-oauth-context");
            final Lock resourceLock = createLockForResourceOwner(resourceOwnerId);
            lock.lock();
            try
            {
                resourceLock.lock();
                try
                {
                    if (!oauthContextStore.containsKey(resourceOwnerId))
                    {
                        resourceOwnerOAuthContext = new ResourceOwnerOAuthContext(createLockForResourceOwner(resourceOwnerId), resourceOwnerId);
                        oauthContextStore.put(resourceOwnerId, resourceOwnerOAuthContext);
                    }
                }
                finally
                {
                    resourceLock.unlock();
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        if (resourceOwnerOAuthContext == null)
        {
            resourceOwnerOAuthContext = oauthContextStore.get(resourceOwnerId);
            resourceOwnerOAuthContext.setRefreshUserOAuthContextLock(createLockForResourceOwner(resourceOwnerId));
        }
        return resourceOwnerOAuthContext;
    }

    private Lock createLockForResourceOwner(String resourceOwnerId)
    {
        return lockFactory.createLock(configName + "-" + resourceOwnerId);
    }

    /**
     * Updates the resource owner oauth context information
     *
     * @param resourceOwnerOAuthContext
     */
    public void updateResourceOwnerOAuthContext(ResourceOwnerOAuthContext resourceOwnerOAuthContext)
    {
        final Lock resourceOwnerContextLock = resourceOwnerOAuthContext.getRefreshUserOAuthContextLock();
        resourceOwnerContextLock.lock();
        try
        {
            oauthContextStore.put(resourceOwnerOAuthContext.getResourceOwnerId(), resourceOwnerOAuthContext);
        }
        finally
        {
            resourceOwnerContextLock.unlock();
        }
    }

    public void clearContextForResourceOwner(String resourceOwnerId)
    {
        final ResourceOwnerOAuthContext resourceOwnerOAuthContext = getContextForResourceOwner(resourceOwnerId);
        if (resourceOwnerOAuthContext != null)
        {
            resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().lock();
            try
            {
                oauthContextStore.remove(resourceOwnerId);
            }
            finally
            {
                resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
            }
        }
    }
}
