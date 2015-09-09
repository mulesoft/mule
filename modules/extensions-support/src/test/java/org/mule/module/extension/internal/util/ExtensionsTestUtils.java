/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.config.MuleManifest;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.ParameterModel;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

import org.apache.commons.lang.ArrayUtils;

public abstract class ExtensionsTestUtils
{

    public static final String HELLO_WORLD = "Hello World!";

    public static ValueResolver getResolver(Object value) throws Exception
    {
        return getResolver(value, null, true);
    }

    public static ValueResolver getResolver(Object value, MuleEvent event, boolean dynamic, Class<?>... extraInterfaces) throws Exception
    {
        ValueResolver resolver;
        if (ArrayUtils.isEmpty(extraInterfaces))
        {
            resolver = mock(ValueResolver.class);
        }
        else
        {
            resolver = mock(ValueResolver.class, withSettings().extraInterfaces(extraInterfaces));
        }

        when(resolver.resolve(event != null ? same(event) : any(MuleEvent.class))).thenReturn(value);
        when(resolver.isDynamic()).thenReturn(dynamic);

        return resolver;
    }

    public static ParameterModel getParameter(String name, Class<?> type)
    {
        ParameterModel parameterModel = mock(ParameterModel.class);
        when(parameterModel.getName()).thenReturn(name);
        when(parameterModel.getType()).thenReturn(DataType.of(type));

        return parameterModel;
    }

    public static void stubRegistryKeys(MuleContext muleContext, final String... keys)
    {
        when(muleContext.getRegistry().get(anyString())).thenAnswer(invocation -> {
            String name = (String) invocation.getArguments()[0];
            if (name != null)
            {
                for (String key : keys)
                {
                    if (name.contains(key))
                    {
                        return null;
                    }
                }
            }

            return RETURNS_DEEP_STUBS.get().answer(invocation);
        });
    }

    public static <C> C getConfigurationFromRegistry(String key, MuleEvent muleEvent) throws Exception
    {
        ExtensionManager extensionManager = muleEvent.getMuleContext().getExtensionManager();
        return (C) extensionManager.getConfiguration(key, muleEvent).getValue();
    }

    public static File getMetaInfDirectory(Class clazz)
    {
        URL classUrl = clazz.getResource(clazz.getSimpleName() + ".class");
        String classPath = classUrl.getPath();
        return new File(String.format("%starget/test-classes/META-INF", classPath.substring(0, classPath.indexOf("target"))));
    }

    public static File createManifestFileIfNecessary(File targetDirectory) throws IOException
    {
        return createManifestFileIfNecessary(targetDirectory, MuleManifest.getManifest());
    }

    public static File createManifestFileIfNecessary(File targetDirectory, Manifest sourceManifest) throws IOException
    {
        File manifestFile = new File(targetDirectory.getPath(), "MANIFEST.MF");
        if (!manifestFile.exists())
        {
            Manifest manifest = new Manifest(sourceManifest);
            try (FileOutputStream fileOutputStream = new FileOutputStream(manifestFile))
            {
                manifest.write(fileOutputStream);
            }
        }
        return manifestFile;
    }
}
