/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

/**
 * Adds {@link #getName} method to an object
 */
public interface NamedObject
{

    /**
     * Gets the name of the object
     * 
     * @return the name of the object
     */
    String getName();
}
