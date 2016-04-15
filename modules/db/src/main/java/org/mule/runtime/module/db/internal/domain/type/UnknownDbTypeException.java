/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

/**
 * Thrown to indicate that a data type ID can not be mapped to a database type
 */
public class UnknownDbTypeException extends RuntimeException
{

    /**
     * Creates an exception for a given unknown type
     *
     * @param typeId type ID
     * @param name type name
     */
    public UnknownDbTypeException(int typeId, String name)
    {
        super(String.format("Unable to find a mapping for type ID: %s Name: %s", typeId, name));
    }

    public UnknownDbTypeException(String name)
    {
        super(String.format("Unable to find a mapping for type: %s", name));
    }
}
