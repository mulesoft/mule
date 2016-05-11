/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.util.SpringXMLUtils;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.util.NameUtils;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * A {@link BeanDefinitionParser} to parse message processors
 * which execute operations implemented through the extensions API.
 * <p>
 * It defines an {@link OperationMessageProcessorFactoryBean} which in turn builds
 * the actual {@link MessageProcessor}
 *
 * @since 3.7.0
 */
final class OperationBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final ExtensionModel extensionModel;
    private final OperationModel operationModel;

    OperationBeanDefinitionParser(ExtensionModel extensionModel, OperationModel operationModel)
    {
        super(OperationMessageProcessorFactoryBean.class);
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, XmlExtensionParserDelegate parserDelegate, ParserContext parserContext)
    {
        withContextClassLoader(getClassLoader(extensionModel), () -> {
            parserDelegate.parseConfigRef(element, builder);

            builder.addConstructorArgValue(extensionModel)
                    .addConstructorArgValue(operationModel)
                    .addConstructorArgValue(parserDelegate.toElementDescriptorBeanDefinition(element))
                    .addConstructorArgValue(parseNestedOperations(element, parserContext))
                    .addConstructorArgReference(OBJECT_MULE_CONTEXT);

        });
    }

    private ManagedMap<String, ManagedList<MessageProcessor>> parseNestedOperations(final Element element, final ParserContext parserContext)
    {
        final ManagedMap<String, ManagedList<MessageProcessor>> nestedOperations = new ManagedMap<>();

        for (final ParameterModel parameterModel : operationModel.getParameterModels())
        {
            final MetadataType type = parameterModel.getType();
            type.accept(new MetadataTypeVisitor()
            {
                @Override
                public void visitObject(ObjectType objectType)
                {
                    if (isOperation(objectType))
                    {
                        addNestedOperation();
                    }
                }

                @Override
                public void visitArrayType(ArrayType arrayType)
                {
                    if (isOperation(arrayType.getType()))
                    {
                        addNestedOperation();
                    }
                }

                private void addNestedOperation()
                {
                    nestedOperations.put(parameterModel.getName(), parseNestedProcessor(element, parameterModel, parserContext));
                }

                private boolean isOperation(MetadataType metadataType)
                {
                    return NestedProcessor.class.isAssignableFrom(JavaTypeUtils.getType(metadataType));
                }
            });
        }

        return nestedOperations;
    }

    private String generateChildBeanName(Element element)
    {
        String id = SpringXMLUtils.getNameOrId(element);
        if (StringUtils.isBlank(id))
        {
            String parentId = SpringXMLUtils.getNameOrId(((Element) element.getParentNode()));
            return String.format(".%s:%s", parentId, element.getLocalName());
        }
        else
        {
            return id;
        }
    }

    private ManagedList<MessageProcessor> parseNestedProcessor(Element element, ParameterModel parameterModel, ParserContext parserContext)
    {
        element = DomUtils.getChildElementByTagName(element, NameUtils.hyphenize(parameterModel.getName()));
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MessageProcessorChainFactoryBean.class);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        String childBeanName = generateChildBeanName(element);
        parserContext.getRegistry().registerBeanDefinition(childBeanName, beanDefinition);
        element.setAttribute("name", childBeanName);

        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        builder.setScope(SCOPE_SINGLETON);

        ManagedList<MessageProcessor> processors = (ManagedList) parserContext.getDelegate().parseListElement(element, builder.getBeanDefinition());
        parserContext.getRegistry().removeBeanDefinition(generateChildBeanName(element));

        return processors;
    }

}
