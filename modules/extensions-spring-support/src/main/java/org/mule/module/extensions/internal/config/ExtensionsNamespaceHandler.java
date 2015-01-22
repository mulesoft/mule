/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.util.NameUtils.getGlobalPojoTypeName;
import static org.mule.module.extensions.internal.util.NameUtils.hyphenize;
import static org.mule.util.Preconditions.checkState;
import org.mule.config.spring.MuleArtifactContext;
import org.mule.extensions.ExtensionsManager;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.DataType;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.Parameter;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.module.extensions.internal.introspection.BaseDataQualifierVisitor;
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
 * For this namespace handler to function, an instance of {@link ExtensionsManager}
 * has to be accessible and the {@link ExtensionsManager#discoverExtensions(ClassLoader)}
 * needs to have successfully discovered and register extensions.
 *
 * @since 3.7.0
 */
public class ExtensionsNamespaceHandler extends NamespaceHandlerSupport
{

    private ExtensionsManager extensionsManager;
    private final Map<String, Extension> handledExtensions = new HashMap<>();
    private final Multimap<Extension, String> topLevelParameters = HashMultimap.create();

    /**
     * Attempts to get a hold on a {@link ExtensionsManager}
     * instance
     *
     * @throws java.lang.IllegalStateException if no extension manager could be found
     */
    @Override
    public void init()
    {
        extensionsManager = MuleArtifactContext.getCurrentMuleContext().get().getExtensionsManager();
        checkState(extensionsManager != null, "Could not obtain handledExtensions manager");
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
            registerBeanDefinitionParser(hyphenize(operation.getName()), new OperationBeanDefinitionParser(operation));
        }
    }

    private void registerConfigurations(Extension extension) throws Exception
    {
        for (Configuration configuration : extension.getConfigurations())
        {
            registerBeanDefinitionParser(configuration.getName(), new ConfigurationBeanDefinitionParser(configuration));
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
        parameterType.getQualifier().accept(new BaseDataQualifierVisitor()
        {

            @Override
            public void onPojo()
            {
                String name = hyphenize(getGlobalPojoTypeName(parameterType));
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
                if (genericTypes == null)
                {
                    return;
                }

                if (genericTypes.length >= 1)
                {
                    DataType keyType = genericTypes[0];
                    registerTopLevelParameter(extension, keyType);
                }

                if (genericTypes.length >= 2)
                {
                    DataType valueType = parameterType.getGenericTypes()[0];
                    valueType.getQualifier().accept(this);
                    registerTopLevelParameter(extension, valueType);
                }
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
        Collection<Extension> capableExtensions = extensionsManager.getExtensionsCapableOf(XmlCapability.class);
        if (CollectionUtils.isEmpty(capableExtensions))
        {
            throw new IllegalArgumentException(
                    String.format("Could not find any handled extensions supporting XML capabilities. Can't process namespace %s", namespace));
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
