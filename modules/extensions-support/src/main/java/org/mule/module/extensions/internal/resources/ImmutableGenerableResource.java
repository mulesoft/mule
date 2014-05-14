/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;

import org.mule.extensions.resources.GenerableResource;

/**
 * Immutable implementation of a {@link GenerableResource
 *
 * @since 3.7.0}
 */
final class ImmutableGenerableResource implements GenerableResource
{

    private final String filepath;
    private final StringBuilder contentBuilder = new StringBuilder();

    ImmutableGenerableResource(String filepath)
    {
        this.filepath = filepath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilePath()
    {
        return filepath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuilder getContentBuilder()
    {
        return contentBuilder;
    }
}
