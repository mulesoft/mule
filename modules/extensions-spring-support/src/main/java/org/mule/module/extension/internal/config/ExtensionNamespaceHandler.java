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
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.introspection.AbstractDataQualifierVisitor;
import org.mule.util.ArrayUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Generic implementation of {@link NamespaceHandler} capable of parsing configurations and operations
 * for any given {@link ExtensionModel} which supports the given namespace.
 * <p>
 * For this namespace handler to function, an instance of {@link ExtensionManager}
 * has to be accessible and the {@link ExtensionManager#discoverExtensions(ClassLoader)}
 * needs to have successfully discovered and register extensions.
 *
 * @since 3.7.0
 */
public class ExtensionNamespaceHandler extends NamespaceHandlerSupport
{

    private ExtensionManager extensionManager;
    private final Map<String, ExtensionModel> handledExtensions = new HashMap<>();
    private final Multimap<ExtensionModel, String> topLevelParameters = HashMultimap.create();
    private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();

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
            ExtensionModel extensionModel = locateExtensionByNamespace(namespace);

            registerTopLevelParameters(extensionModel);
            registerConfigurations(extensionModel);
            registerOperations(extensionModel);
            registerConnectionProviders(extensionModel);
            registerMessageSources(extensionModel);

            handledExtensions.put(namespace, extensionModel);
        }
        catch (Exception e)
        {
            parserContext.getReaderContext().fatal(e.getMessage(), element, e);
        }
    }

    private void registerOperations(ExtensionModel extensionModel)
    {
        extensionModel.getOperationModels().forEach(operationModel -> registerParser(hyphenize(operationModel.getName()), new OperationBeanDefinitionParser(extensionModel, operationModel)));
    }

    private void registerConfigurations(ExtensionModel extensionModel)
    {
        extensionModel.getConfigurationModels().forEach(configurationModel -> registerParser(configurationModel.getName(), new ConfigurationBeanDefinitionParser(configurationModel)));
    }

    private void registerConnectionProviders(ExtensionModel extensionModel)
    {
        extensionModel.getConnectionProviders().forEach(providerModel -> registerParser(providerModel.getName(), new ConnectionProviderBeanDefinitionParser(providerModel)));
    }

    private void registerMessageSources(ExtensionModel extensionModel)
    {
        extensionModel.getSourceModels().forEach(sourceModel -> registerParser(hyphenize(sourceModel.getName()), new SourceBeanDefinitionParser(extensionModel, sourceModel)));
    }

    private void registerTopLevelParameters(ExtensionModel extensionModel)
    {
        extensionModel.getConfigurationModels().forEach(configurationModel -> registerTopLevelParameter(extensionModel, configurationModel.getParameterModels()));
        extensionModel.getOperationModels().forEach(operationModel -> registerTopLevelParameter(extensionModel, operationModel.getParameterModels()));
    }

    private void registerTopLevelParameter(final ExtensionModel extensionModel, final DataType parameterType)
    {
        parameterType.getQualifier().accept(new AbstractDataQualifierVisitor()
        {

            @Override
            public void onPojo()
            {
                String name = hyphenize(getTopLevelTypeName(parameterType));
                if (topLevelParameters.put(extensionModel, name))
                {
                    registerParser(name, new TopLevelParameterTypeBeanDefinitionParser(parameterType));
                }
            }

            @Override
            public void onList()
            {
                if (!ArrayUtils.isEmpty(parameterType.getGenericTypes()))
                {
                    registerTopLevelParameter(extensionModel, parameterType.getGenericTypes()[0]);
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
                registerTopLevelParameter(extensionModel, keyType);
            }
        });

    }

    private void registerTopLevelParameter(ExtensionModel extensionModel, Collection<ParameterModel> parameterModels)
    {
        parameterModels.forEach(parameterModel -> registerTopLevelParameter(extensionModel, parameterModel.getType()));
    }


    private ExtensionModel locateExtensionByNamespace(String namespace)
    {
        for (ExtensionModel extensionModel : extensionManager.getExtensions())
        {
            XmlModelProperty xmlProperty = extensionModel.getModelProperty(XmlModelProperty.KEY);
            if (namespace.equals(xmlProperty.getSchemaLocation()))
            {
                return extensionModel;
            }
        }

        throw new IllegalArgumentException(String.format("Could not find any extension associated to namespace %s", namespace));
    }

    private void registerParser(String elementName, BeanDefinitionParser parser)
    {
        registerBeanDefinitionParser(elementName, parser);
        parsers.put(elementName, parser);
    }

    /**
     * Locates a registered {@link BeanDefinitionParser} which can parse an element
     * of the given {@code elementName}.
     *
     * @param elementName the name of the element for which you want to locate a parser
     * @return A {@link BeanDefinitionParser} or {@code null} if no such parser is registered on {@code this} instance
     */
    BeanDefinitionParser getParser(String elementName)
    {
        return parsers.get(elementName);
    }
}
