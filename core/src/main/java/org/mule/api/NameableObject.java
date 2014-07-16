/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

/**
 * Adds {@link #getName} and {@link #setName} methods to an object
 */
public interface NameableObject extends NamedObject
{
    /**
     * Sets the name of the object
     * 
     * @param name the name of the object
     */
    void setName(String name);
}
