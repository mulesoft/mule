/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

/**
 * Determines if a given class or resource is exported in a plugin classloader
 */
public interface ClassLoaderFilter
{

    /**
     * Determines if a given name must be accepted or filtered.
     *
     * @param name class/resource name to check
     * @return true if the name is accepted and false if must be filtered
     */
    boolean accepts(String name);
}
