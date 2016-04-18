/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.metadata.java.JavaTypeLoader.JAVA;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeHandlerManagerFactory;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.TypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.handler.TypeHandlerManager;
import org.mule.metadata.java.utils.ParsingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;

import org.apache.commons.lang.ArrayUtils;

public abstract class ExtensionsTestUtils
{

    public static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    public static final BaseTypeBuilder<?> TYPE_BUILDER = BaseTypeBuilder.create(JAVA);

    public static final String HELLO_WORLD = "Hello World!";

    public static MetadataType toMetadataType(Class<?> type)
    {
        return TYPE_LOADER.load(type);
    }

    public static ArrayType arrayOf(Class<? extends Collection> clazz, TypeBuilder itemType)
    {
        ArrayTypeBuilder<?> arrayTypeBuilder = TYPE_BUILDER.arrayType();
        arrayTypeBuilder.id(clazz.getName());
        arrayTypeBuilder.of(itemType);

        return arrayTypeBuilder.build();
    }

    public static DictionaryType dictionaryOf(Class<? extends Map> clazz, TypeBuilder<?> keyTypeBuilder, TypeBuilder<?> valueTypeBuilder)
    {
        return TYPE_BUILDER.dictionaryType().id(clazz.getName())
                .ofKey(keyTypeBuilder)
                .ofValue(valueTypeBuilder)
                .build();
    }

    public static TypeBuilder<?> objectTypeBuilder(Class<?> clazz)
    {
        BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
        final TypeHandlerManager typeHandlerManager = new ExtensionsTypeHandlerManagerFactory().createTypeHandlerManager();
        typeHandlerManager.handle(clazz, new ParsingContext(), typeBuilder);

        return typeBuilder;
    }

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
        when(parameterModel.getType()).thenReturn(toMetadataType(type));
        when(parameterModel.getModelProperty(any())).thenReturn(Optional.empty());

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
