/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.internal.DefaultMuleExtensionBuilder;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionDescriber;
import org.mule.extensions.spi.MuleExtensionScanner;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public final class DefaultMuleExtensionScanner implements MuleExtensionScanner
{

    private ServiceLoader<MuleExtensionDescriber> scanner;

    @Override
    public List<MuleExtension> scan()
    {
        List<MuleExtension> extensions = new LinkedList<MuleExtension>();

        Iterator<MuleExtensionDescriber> describerIterator = getScanner().iterator();

        while (describerIterator.hasNext())
        {
            MuleExtensionBuilder builder = DefaultMuleExtensionBuilder.newBuilder();
            describerIterator.next().describe(builder);
            extensions.add(builder.build());
        }

        return ImmutableList.copyOf(extensions);
    }

    @Override
    public int scanAndRegister(MuleExtensionsManager muleExtensionsManager)
    {
        List<MuleExtension> extensions = scan();
        for (MuleExtension extension : extensions)
        {
            muleExtensionsManager.register(extension);
        }

        return extensions.size();
    }

    private synchronized ServiceLoader<MuleExtensionDescriber> getScanner()
    {
        if (scanner == null)
        {
            scanner = ServiceLoader.load(MuleExtensionDescriber.class);
        }

        return scanner;
    }
}
