/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
