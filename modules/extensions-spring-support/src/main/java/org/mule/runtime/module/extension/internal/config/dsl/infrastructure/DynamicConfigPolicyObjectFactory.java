/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.infrastructure;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;

/**
 * A {@link ObjectFactory} instance that produces {@link DynamicConfigPolicy} instances.
 * <p>
 * If a {@link #expirationPolicy} was not set, then {@link DynamicConfigPolicy#getDefault(TimeSupplier)}
 * is used to produce an instance.
 *
 * @since 4.0
 */
public class DynamicConfigPolicyObjectFactory implements ObjectFactory<DynamicConfigPolicy>
{

    private final MuleContext muleContext;
    private ExpirationPolicy expirationPolicy;

    public DynamicConfigPolicyObjectFactory(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public DynamicConfigPolicy getObject() throws Exception
    {
        if (expirationPolicy != null)
        {
            return new DynamicConfigPolicy(expirationPolicy);
        }

        return DynamicConfigPolicy.getDefault(TimeSupplier.getDefault(muleContext));
    }

    public void setExpirationPolicy(ExpirationPolicy expirationPolicy)
    {
        this.expirationPolicy = expirationPolicy;
    }
}
