/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.ExpirationPolicy;
import org.mule.util.Preconditions;

/**
 * A policy for how the platform should handle dynamic configuration instances
 *
 * @since 4.0
 */
public final class DynamicConfigPolicy
{

    public static DynamicConfigPolicy DEFAULT = new DynamicConfigPolicy(ImmutableExpirationPolicy.DEFAULT);

    private final ExpirationPolicy expirationPolicy;

    /**
     * Creates a new instance.
     *
     * @param expirationPolicy the expiration policy to be used.
     * @throws IllegalArgumentException is {@code expirationPolicy} is {@code null}
     */
    public DynamicConfigPolicy(ExpirationPolicy expirationPolicy)
    {
        Preconditions.checkArgument(expirationPolicy != null, "expiration policy cannot be null");
        this.expirationPolicy = expirationPolicy;
    }

    /**
     * Returns the {@link ExpirationPolicy} for the dynamic configuration instances
     *
     * @return a {@link ExpirationPolicy}. It will never be {@code null}
     */
    public ExpirationPolicy getExpirationPolicy()
    {
        return expirationPolicy;
    }
}
