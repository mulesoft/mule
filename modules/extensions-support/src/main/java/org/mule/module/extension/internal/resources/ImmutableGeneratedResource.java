/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.resources;

import org.mule.extension.resources.GeneratedResource;

/**
 * Immutable implementation of a {@link GeneratedResource
 *
 * @since 3.7.0}
 */
final class ImmutableGeneratedResource implements GeneratedResource
{

    private final String filepath;
    private final StringBuilder contentBuilder = new StringBuilder();

    ImmutableGeneratedResource(String filepath)
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
