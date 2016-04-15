/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.core.time.Time;

/**
 * Default implementation of {@link ExtensionConfig}
 *
 * @since 4.0
 */
public final class DefaultExtensionConfig implements ExtensionConfig
{
    private Time dynamicConfigExpirationFrequency;

    @Override
    public Time getDynamicConfigExpirationFrequency()
    {
        return dynamicConfigExpirationFrequency;
    }

    public void setDynamicConfigExpirationFrequency(Time dynamicConfigExpirationFrequency)
    {
        this.dynamicConfigExpirationFrequency = dynamicConfigExpirationFrequency;
    }
}
