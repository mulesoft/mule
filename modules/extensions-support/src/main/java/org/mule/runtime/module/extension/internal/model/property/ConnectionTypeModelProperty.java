/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;


import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;

/**
 * An immutable  model property which specifies that the owning {@link EnrichableModel}
 * requires a connection of a given {@link #connectionType}
 *
 * @since 4.0
 */
public final class ConnectionTypeModelProperty implements ModelProperty
{

    private final MetadataType connectionType;

    /**
     * Creates a new instance for the given {@code connectionType}
     *
     * @param connectionType
     */
    public ConnectionTypeModelProperty(MetadataType connectionType)
    {
        this.connectionType = connectionType;
    }

    /**
     * @return the {@link {@link #connectionType}}
     */
    public MetadataType getConnectionType()
    {
        return connectionType;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code connectionType}
     */
    @Override
    public String getName()
    {
        return "connectionType";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false}
     */
    @Override
    public boolean isExternalizable()
    {
        return false;
    }
}
