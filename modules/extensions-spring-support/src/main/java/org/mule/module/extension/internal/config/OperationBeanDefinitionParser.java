/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_CONFIG;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.setNoRecurseOnDefinition;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.enricher.MessageEnricher;
import org.mule.extension.introspection.DataQualifier;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.introspection.AbstractDataQualifierVisitor;
import org.mule.module.extension.internal.util.NameUtils;
import org.mule.util.ArrayUtils;

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
 * <p/>
 * It defines an {@link OperationMessageProcessorFactoryBean} which in turn builds
 * the actual {@link MessageProcessor}
 *
 * @since 3.7.0
 */
final class OperationBeanDefinitionParser implements BeanDefinitionParser
{

    private final Extension extension;
    private final Operation operation;

    OperationBeanDefinitionParser(Extension extension, Operation operation)
    {
        this.extension = extension;
        this.operation = operation;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OperationMessageProcessorFactoryBean.class);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        parseConfigRef(element, builder);
        builder.addConstructorArgValue(extension);
        builder.addConstructorArgValue(operation);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
        builder.addConstructorArgValue(parseNestedOperations(element, parserContext));
        builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);

        BeanDefinition definition = builder.getBeanDefinition();
        setNoRecurseOnDefinition(definition);
        attachProcessorDefinition(parserContext, definition);

        return definition;
    }

    private ManagedMap<String, ManagedList<MessageProcessor>> parseNestedOperations(final Element element, final ParserContext parserContext)
    {
        final ManagedMap<String, ManagedList<MessageProcessor>> nestedOperations = new ManagedMap<>();

        for (final Parameter parameter : operation.getParameters())
        {
            final DataType type = parameter.getType();
            type.getQualifier().accept(new AbstractDataQualifierVisitor()
            {

                @Override
                public void onOperation()
                {
                    nestedOperations.put(parameter.getName(), parseNestedProcessor(element, parameter, parserContext));
                }

                @Override
                public void onList()
                {
                    DataType[] generics = type.getGenericTypes();
                    if (!ArrayUtils.isEmpty(generics) && generics[0].getQualifier() == DataQualifier.OPERATION)
                    {
                        nestedOperations.put(parameter.getName(), parseNestedProcessor(element, parameter, parserContext));
                    }
                }
            });
        }

        return nestedOperations;
    }

    private void parseConfigRef(Element element, BeanDefinitionBuilder builder)
    {
        String configRef = element.getAttribute(ATTRIBUTE_NAME_CONFIG);
        if (StringUtils.isBlank(configRef))
        {
            configRef = null;
        }

        builder.addConstructorArgValue(configRef);
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

    private ManagedList<MessageProcessor> parseNestedProcessor(Element element, Parameter parameter, ParserContext parserContext)
    {
        element = DomUtils.getChildElementByTagName(element, NameUtils.hyphenize(parameter.getName()));
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MessageProcessorChainFactoryBean.class);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        String childBeanName = generateChildBeanName(element);
        parserContext.getRegistry().registerBeanDefinition(childBeanName, beanDefinition);
        element.setAttribute("name", childBeanName);

        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        ManagedList<MessageProcessor> processors = (ManagedList) parserContext.getDelegate().parseListElement(element, builder.getBeanDefinition());
        parserContext.getRegistry().removeBeanDefinition(generateChildBeanName(element));

        return processors;
    }

    private void attachProcessorDefinition(ParserContext parserContext, BeanDefinition definition)
    {
        MutablePropertyValues propertyValues = parserContext.getContainingBeanDefinition()
                .getPropertyValues();
        if (parserContext.getContainingBeanDefinition()
                .getBeanClassName()
                .equals(PollingMessageSourceFactoryBean.class.getName()))
        {

            propertyValues.addPropertyValue("messageProcessor", definition);
        }
        else
        {
            if (parserContext.getContainingBeanDefinition()
                    .getBeanClassName()
                    .equals(MessageEnricher.class.getName()))
            {
                propertyValues.addPropertyValue("enrichmentMessageProcessor", definition);
            }
            else
            {
                PropertyValue messageProcessors = propertyValues.getPropertyValue("messageProcessors");
                if ((messageProcessors == null) || (messageProcessors.getValue() == null))
                {
                    propertyValues.addPropertyValue("messageProcessors", new ManagedList());
                }
                List listMessageProcessors = ((List) propertyValues.getPropertyValue("messageProcessors").getValue());
                listMessageProcessors.add(definition);
            }
        }
    }

    private void attachSourceDefinition(ParserContext parserContext, BeanDefinition definition)
    {
        MutablePropertyValues propertyValues = parserContext.getContainingBeanDefinition()
                .getPropertyValues();
        propertyValues.addPropertyValue("messageSource", definition);
    }
}
