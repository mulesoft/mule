/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.state;

import org.mule.util.lock.LockFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the OAuth context for a particular config
 */
public class ConfigOAuthContext
{

    private final LockFactory updateOAuthStateLockFactory;
    private final String configName;
    private Map<String, UserOAuthContext> oauthStatePerUser = new HashMap<String, UserOAuthContext>();

    public ConfigOAuthContext(final LockFactory lockFactory, final String configName)
    {
        updateOAuthStateLockFactory = lockFactory;
        this.configName = configName;
    }

    /**
     * Retrieves the oauth context for a particular user. If there's no state for that user a new state is retrieve so never returns null.
     *
     * @param userId id of the user.
     * @return oauth state
     */
    public UserOAuthContext getContextForUser(final String userId)
    {
        if (!oauthStatePerUser.containsKey(userId))
        {
            oauthStatePerUser.put(userId, new UserOAuthContext(updateOAuthStateLockFactory.createLock(configName + "-" + userId), userId));
        }
        return oauthStatePerUser.get(userId);
    }
}
