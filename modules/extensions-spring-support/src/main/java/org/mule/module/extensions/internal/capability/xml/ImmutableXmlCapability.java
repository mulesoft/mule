/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import org.mule.extensions.introspection.capability.XmlCapability;

/**
 * Immutable implementation of {@link org.mule.extensions.introspection.capability.XmlCapability}
 *
 * @since 3.7.0
 */
public final class ImmutableXmlCapability implements XmlCapability
{

    private final String schemaVersion;
    private final String namespace;
    private final String schemaLocation;

    public ImmutableXmlCapability(String schemaVersion, String namespace, String schemaLocation)
    {
        this.schemaVersion = schemaVersion;
        this.namespace = namespace;
        this.schemaLocation = schemaLocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSchemaVersion()
    {
        return schemaVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSchemaLocation()
    {
        return schemaLocation;
    }
}
