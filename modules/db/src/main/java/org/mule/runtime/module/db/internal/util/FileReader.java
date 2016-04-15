/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.util;

/**
 * Abstracts file reading operations
 */
public interface FileReader
{

    /**
     * Attempts to load a resource from the file system, from a URL, or from the
     * classpath, in that order.
     *
     * @param resourceName The name of the resource to load
     * @return the requested resource as a string
     * @throws java.io.IOException in case of an IO error
     */
    String getResourceAsString(String resourceName) throws java.io.IOException;
}
