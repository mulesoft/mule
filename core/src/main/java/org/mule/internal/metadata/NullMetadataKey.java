/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.metadata;


import org.mule.api.metadata.MetadataKey;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Null {@link MetadataKey} implementation that represents the absence of a key
 *
 * @since 1.0
 */
public final class NullMetadataKey implements MetadataKey
{

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getProperty(String propertyId)
    {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getProperties()
    {
        return Collections.emptyMap();
    }
}
