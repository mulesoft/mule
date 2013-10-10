/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.util.scan.ClasspathScanner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.IBeanGroup;
import org.ibeans.annotation.Template;

/**
 * A configuration builder that registers iBean objects on the classpath with the Mule registry.
 * <p/>
 * The registry can then be used to query available iBeans.
 */
public class IBeanHolderConfigurationBuilder extends AbstractAnnotationConfigurationBuilder
{
    public static final String IBEAN_HOLDER_PREFIX = "_ibeanHolder.";

    public IBeanHolderConfigurationBuilder()
    {
        super();
    }

    public IBeanHolderConfigurationBuilder(String... basepackages)
    {
        super(basepackages);
    }

    public IBeanHolderConfigurationBuilder(ClassLoader classLoader)
    {
        super(classLoader);
    }

    public IBeanHolderConfigurationBuilder(ClassLoader classLoader, String... basepackages)
    {
        super(classLoader, basepackages);
    }

    @Override
    protected String getScanPackagesProperty()
    {
        return "ibeans.scan.packages";
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        Set<Class<?>> ibeanClasses = new HashSet<Class<?>>();
        ClasspathScanner scanner = createClasspathScanner();

        try
        {
            //There will be some overlap here but only
            ibeanClasses.addAll(scanner.scanFor(Call.class, ClasspathScanner.INCLUDE_INTERFACE));
            ibeanClasses.addAll(scanner.scanFor(Template.class, ClasspathScanner.INCLUDE_INTERFACE));
            //Some ibeans will extend other iBeans but have not methods of there own
            ibeanClasses.addAll(scanner.scanFor(IBeanGroup.class, ClasspathScanner.INCLUDE_INTERFACE));
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }

        for (Class<?> ibeanClass : ibeanClasses)
        {
            muleContext.getRegistry().registerObject(IBeanHolder.getId(ibeanClass), new IBeanHolder(ibeanClass));
        }
    }
}
