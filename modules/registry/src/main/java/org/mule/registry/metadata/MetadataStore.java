/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry.metadata;

import java.util.HashMap;

public class MetadataStore
{
    private static HashMap metadata = new HashMap();

    public static void addObjectMetadata(ObjectMetadata om)
    {
        if (!metadata.containsKey(om.getClassName()))
        {
            metadata.put(om.getClassName(), om);
        }
    }

    /*
     * Returns the Metadata object describing a Mule object
     */
    public static ObjectMetadata getObjectMetadata(String className) throws MissingMetadataException
    {
        if (!metadata.containsKey(className))
            throw new MissingMetadataException(className);
        return (ObjectMetadata)metadata.get(className);
    }

    /*
     * Returns the Metadata object describing a property of a Mule object
     */
    public static PropertyMetadata getPropertyMetadata(String className, String propertyName) throws MissingMetadataException
    {
        if (!metadata.containsKey(className))
            throw new MissingMetadataException(className);
        if (!((ObjectMetadata)metadata.get(className)).getProperties().containsKey(propertyName))
            throw new MissingMetadataException(className, propertyName);
        return (PropertyMetadata)((ObjectMetadata)metadata.get(className)).getProperty(propertyName);
    }
}
