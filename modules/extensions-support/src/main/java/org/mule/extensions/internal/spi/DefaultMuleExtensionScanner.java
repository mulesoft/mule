/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.internal.DefaultMuleExtensionBuilder;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.spi.MuleExtensionBuilder;
import org.mule.extensions.spi.MuleExtensionScanner;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public final class DefaultMuleExtensionScanner implements MuleExtensionScanner
{

    private static final String EXTENSIONS_PROPERTIES = "META-INF/extensions/mule.extensions";

    @Override
    public List<MuleExtension> scan()
    {
        Enumeration<URL> allExtensions = ClassUtils.getResources(EXTENSIONS_PROPERTIES, getClass());
        List<MuleExtension> extensions = new LinkedList<MuleExtension>();

        while (allExtensions.hasMoreElements())
        {
            String lines = read(allExtensions.nextElement());
            for (String line : lines.split(System.lineSeparator()))
            {

                if (StringUtils.isBlank(line))
                {
                    continue; // skip empty line
                }

                extensions.add(loadExtension(line));
            }
        }

        return ImmutableList.copyOf(extensions);
    }

    @Override
    public List<MuleExtension> scanAndRegister(MuleExtensionsManager muleExtensionsManager)
    {
        List<MuleExtension> extensions = scan();
        for (MuleExtension extension : extensions)
        {
            muleExtensionsManager.register(extension);
        }

        return extensions;
    }

    private MuleExtension loadExtension(String extensionClassname)
    {
        try
        {
            Class<?> extensionClass = ClassUtils.loadClass(extensionClassname.trim(), getClass());

            MuleExtensionBuilder builder = DefaultMuleExtensionBuilder.newBuilder();
            new DefaultMuleExtensionDescriber(extensionClass).describe(builder);

            return builder.build();
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException(String.format("Extension %s was declared but not found in classpath", extensionClassname), e);
        }
    }

    private String read(URL url)
    {
        InputStream in = null;
        try
        {
            in = url.openStream();
            return IOUtils.toString(in);
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Could not read extension descriptor at " + url.getPath()), e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

}
