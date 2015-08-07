/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.module.extension.internal.util.NameUtils.hyphenize;
import static org.mule.util.Preconditions.checkState;
import org.mule.config.spring.MuleArtifactContext;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.module.extension.internal.introspection.AbstractDataQualifierVisitor;
import org.mule.util.ArrayUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Generic implementation of {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * capable of parsing configurations and operations for any given {@link Extension}
 * which supports the given namespace.
 * <p/>
 * For this namespace handler to function, an instance of {@link ExtensionManager}
 * has to be accessible and the {@link ExtensionManager#discoverExtensions(ClassLoader)}
 * needs to have successfully discovered and register extensions.
 *
 * @since 3.7.0
 */
public class ExtensionsNamespaceHandler extends NamespaceHandlerSupport
{

    private ExtensionManager extensionManager;
    private final Map<String, Extension> handledExtensions = new HashMap<>();
    private final Multimap<Extension, String> topLevelParameters = HashMultimap.create();

    /**
     * Attempts to get a hold on a {@link ExtensionManager}
     * instance
     *
     * @throws java.lang.IllegalStateException if no extension manager could be found
     */
    @Override
    public void init()
    {
        extensionManager = MuleArtifactContext.getCurrentMuleContext().get().getExtensionManager();
        checkState(extensionManager != null, "Could not obtain the ExtensionManager");
    }

    /**
     * Registers parsers for the given element, provided that an extension can be found which
     * responds to that namespace
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        String namespace = element.getNamespaceURI();
        if (!isHandled(namespace))
        {
            registerExtensionParsers(namespace, element, parserContext);
        }

        return super.parse(element, parserContext);
    }

    private boolean isHandled(String namespace)
    {
        return handledExtensions.containsKey(namespace);
    }

    private void registerExtensionParsers(String namespace, Element element, ParserContext parserContext)
    {
        try
        {
            Extension extension = locateExtensionByNamespace(namespace);

            registerTopLevelParameters(extension);
            registerConfigurations(extension);
            registerOperations(extension);

            handledExtensions.put(namespace, extension);
        }
        catch (Exception e)
        {
            parserContext.getReaderContext().fatal(e.getMessage(), element, e);
        }
    }

    private void registerOperations(Extension extension) throws Exception
    {
        for (Operation operation : extension.getOperations())
        {
            registerBeanDefinitionParser(hyphenize(operation.getName()), new OperationBeanDefinitionParser(extension, operation));
        }
    }

    private void registerConfigurations(Extension extension) throws Exception
    {
        for (Configuration configuration : extension.getConfigurations())
        {
            registerBeanDefinitionParser(configuration.getName(), new ConfigurationBeanDefinitionParser(extension, configuration));
        }
    }

    private void registerTopLevelParameters(Extension extension)
    {
        for (Configuration configuration : extension.getConfigurations())
        {
            registerTopLevelParameter(extension, configuration.getParameters());
        }

        for (Operation operation : extension.getOperations())
        {
            registerTopLevelParameter(extension, operation.getParameters());
        }
    }

    private void registerTopLevelParameter(final Extension extension, final DataType parameterType)
    {
        parameterType.getQualifier().accept(new AbstractDataQualifierVisitor()
        {

            @Override
            public void onPojo()
            {
                String name = hyphenize(getTopLevelTypeName(parameterType));
                if (topLevelParameters.put(extension, name))
                {
                    registerBeanDefinitionParser(name, new TopLevelParameterTypeBeanDefinitionParser(parameterType));
                }
            }

            @Override
            public void onList()
            {
                if (!ArrayUtils.isEmpty(parameterType.getGenericTypes()))
                {
                    registerTopLevelParameter(extension, parameterType.getGenericTypes()[0]);
                }
            }

            @Override
            public void onMap()
            {
                DataType[] genericTypes = parameterType.getGenericTypes();
                if (ArrayUtils.isEmpty(genericTypes))
                {
                    return;
                }

                DataType keyType = genericTypes[0];
                keyType.getQualifier().accept(this);
                registerTopLevelParameter(extension, keyType);
            }
        });

    }

    private void registerTopLevelParameter(Extension extension, Collection<Parameter> parameters)
    {
        for (final Parameter parameter : parameters)
        {
            registerTopLevelParameter(extension, parameter.getType());
        }
    }


    private Extension locateExtensionByNamespace(String namespace)
    {
        Collection<Extension> capableExtensions = extensionManager.getExtensionsCapableOf(XmlCapability.class);
        if (CollectionUtils.isEmpty(capableExtensions))
        {
            throw new IllegalArgumentException(
                    String.format("Could not find any extensions supporting XML capabilities. Can't process namespace %s", namespace));
        }

        for (Extension extension : capableExtensions)
        {
            XmlCapability capability = extension.getCapabilities(XmlCapability.class).iterator().next();
            if (namespace.equals(capability.getSchemaLocation()))
            {
                return extension;
            }
        }

        throw new IllegalArgumentException(String.format("Could not find extension associated to namespace %s", namespace));
    }
}
