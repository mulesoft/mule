/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader.exception;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;

import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;

import java.util.List;

/**
 * Extends {@link ClassNotFoundException}, composing the individual exceptions of each place where
 * the class was looked for and wasn't found.
 */
public class CompositeClassNotFoundException extends ClassNotFoundException
{

    private static final long serialVersionUID = -6941980241656380059L;

    private String className;
    private ClassLoaderLookupStrategy lookupStrategy;
    private List<ClassNotFoundException> exceptions;

    /**
     * Builds the exception.
     * 
     * @param className the name of the class that was trying to be loaded.
     * @param lookupStrategy the lookupStrategy that was used to load the class.
     * @param exceptions the exceptions thrown by each individual classloader that was used for the
     *            loading.
     */
    public CompositeClassNotFoundException(String className, ClassLoaderLookupStrategy lookupStrategy, List<ClassNotFoundException> exceptions)
    {
        super(format("Cannot load class '%s'", className));
        this.className = className;
        this.lookupStrategy = lookupStrategy;
        this.exceptions = copyOf(exceptions);
    }

    /**
     * @return the name of the class that was trying to be loaded
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * @return the lookupStrategy that was used to load the class.
     */
    public ClassLoaderLookupStrategy getLookupStrategy()
    {
        return lookupStrategy;
    }

    /**
     * @return the exceptions thrown by each individual classloader that was used for the loading.
     */
    public List<ClassNotFoundException> getExceptions()
    {
        return exceptions;
    }
}
