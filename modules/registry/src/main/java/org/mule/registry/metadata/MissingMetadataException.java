/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.metadata;

import org.mule.MuleException;

public class MissingMetadataException extends MuleException
{
    public MissingMetadataException(String className)
    {
        super("No Metadata found for object: " + className);
    }

    public MissingMetadataException(String className, String propertyName)
    {
        super("No Metadata found for object: " + className + 
                ", property: " + propertyName);
    }
}


