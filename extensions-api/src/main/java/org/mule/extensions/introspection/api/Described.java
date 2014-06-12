/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * A generic contract for a class that has a name and a description.
 * Implementations might use {@link #getName()} as key, but they're not required to.
 *
 * @since 1.0
 */
public interface Described
{

    /**
     * Returns the component's name
     *
     * @return a non blank {@link java.lang.String}
     */
    String getName();

    /**
     * Returns the component's description
     *
     * @return a non blank {@link java.lang.String}
     */
    String getDescription();
}
