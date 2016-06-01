/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader.exception;

import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.FineGrainedControlClassLoader;

/**
 * Wraps a {@link ClassNotFoundException} thrown by a delegate classloader of
 * {@link FineGrainedControlClassLoader}, providing additional troubleshooting information.
 */
public class FineGrainedClassNotFoundException extends ClassNotFoundException
{

    private static final long serialVersionUID = 1590142936781467994L;

    private ClassLoaderLookupStrategy lookupStrategy;

    /**
     * Builds the exception.
     * 
     * @param cause the exception thrown by the delegated classloader.
     * @param lookupStrategy the lookupStrategy that was used to load the class.
     */
    public FineGrainedClassNotFoundException(ClassNotFoundException cause, ClassLoaderLookupStrategy lookupStrategy)
    {
        super(cause.getMessage(), cause);
        this.lookupStrategy = lookupStrategy;
    }

    /**
     * @return the lookupStrategy that was used to load the class.
     */
    public ClassLoaderLookupStrategy getLookupStrategy()
    {
        return lookupStrategy;
    }
}
