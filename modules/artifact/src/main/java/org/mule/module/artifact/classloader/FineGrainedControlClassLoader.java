/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import java.net.URL;

/**
 * Defines a {@link ClassLoader} which enables the control of the class
 * loading lookup mode.
 *
 * <p/>
 * By using a {@link ClassLoaderLookupPolicy} this classLoader can use
 * parent-first or child-first classloading lookup mode per package.
 */
public class FineGrainedControlClassLoader extends GoodCitizenClassLoader
{

    private final ClassLoaderLookupPolicy lookupPolicy;

    public FineGrainedControlClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY);
    }

    public FineGrainedControlClassLoader(URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy)
    {
        super(urls, parent);
        this.lookupPolicy = lookupPolicy;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> result = findLoadedClass(name);

        if (result != null)
        {
            return result;
        }
        boolean overrideMatch = lookupPolicy.isOverridden(name) || lookupPolicy.isBlocked(name);

        if (overrideMatch)
        {
            boolean blockedMatch = lookupPolicy.isBlocked(name);

            if (blockedMatch)
            {
                // load this class from the child ONLY, don't attempt parent, let CNFE exception propagate
                result = findClass(name);
            }
            else
            {
                // load this class from the child
                try
                {
                    result = findClass(name);
                }
                catch (ClassNotFoundException e)
                {
                    // let it fail with CNFE
                    result = findParentClass(name);
                }
            }
        }
        else
        {
            // no overrides, regular parent-first lookup
            try
            {
                result = findParentClass(name);
            }
            catch (ClassNotFoundException e)
            {
                result = findClass(name);
            }
        }

        if (resolve)
        {
            resolveClass(result);
        }

        return result;
    }

    protected Class<?> findParentClass(String name) throws ClassNotFoundException
    {
        if (getParent() != null)
        {
            return getParent().loadClass(name);
        }
        else
        {
            return findSystemClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return super.findClass(name);
    }
}
