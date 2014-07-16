/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.SimpleServiceFactoryBean;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SimpleServiceDefinitionParser extends AbstractFlowConstructDefinitionParser
{
    private static final String COMPONENT_CLASS_ATTRIBUTE = "component-class";
    private static final String COMPONENT_REF_ATTRIBUTE = "component-ref";
    private static final String COMPONENT_CHILD_TYPE = "componentType";
    private static final String ABSTRACT_ATTRIBUTE = "abstract";

    public SimpleServiceDefinitionParser()
    {
        super.addAlias("endpoint", "endpointBuilder");
        
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{ADDRESS_ATTRIBUTE}, new String[]{ENDPOINT_REF_ATTRIBUTE}}));
        
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{COMPONENT_CLASS_ATTRIBUTE}, new String[]{COMPONENT_REF_ATTRIBUTE}}));
        
        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            ENDPOINT_REF_ATTRIBUTE, ADDRESS_ATTRIBUTE, TRANSFORMER_REFS_ATTRIBUTE,
            RESPONSE_TRANSFORMER_REFS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_CHILD}));

        // We cannot support component element inheritance because components are singletons
        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            ABSTRACT_ATTRIBUTE}, new String[]{COMPONENT_CHILD_TYPE}));

    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return SimpleServiceFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        final String componentRefAttribute = element.getAttribute(COMPONENT_REF_ATTRIBUTE);
        if (StringUtils.isNotBlank(componentRefAttribute))
        {
            builder.addPropertyValue("componentBeanName", componentRefAttribute);
        }
        element.removeAttribute(COMPONENT_REF_ATTRIBUTE);

        super.doParse(element, parserContext, builder);
    }
}
