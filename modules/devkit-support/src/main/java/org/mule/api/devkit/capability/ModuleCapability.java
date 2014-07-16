/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.devkit.capability;

import javax.resource.spi.ConnectionManager;

/**
 * Enumeration of possible capabilities of Mule modules. Each capability represents a
 * bit in a bit array. The capabilities of a particular module can be queried using
 * {@link Capabilities}
 */
public enum ModuleCapability
{

    /**
     * This capability indicates that the module implements
     * {@link org.mule.api.lifecycle.Lifecycle}
     */
    LIFECYCLE_CAPABLE(0),

    /**
     * This capability indicates that the module implements {@link ConnectionManager}
     */
    CONNECTION_MANAGEMENT_CAPABLE(1),

    /**
     * This capability indicates that the module implements
     * {@link org.mule.api.oauth.OAuth1Adapter}
     */
    OAUTH1_CAPABLE(2),

    /**
     * This capability indicates that the module implements
     * {@link org.mule.api.oauth.OAuth2Adapter}
     */
    OAUTH2_CAPABLE(3),

    /**
     * This capability indicates that the module implements
     * {@link org.mule.api.adapter.PoolManager}
     */
    POOLING_CAPABLE(4),

    /**
     * This capability indicates that the module implements
     * {@link org.mule.api.oauth.OAuthManager}
     */
    OAUTH_ACCESS_TOKEN_MANAGEMENT_CAPABLE(5);

    private int bit;

    ModuleCapability(int bit)
    {
        this.bit = bit;
    }

    public int getBit()
    {
        return this.bit;
    }

}
