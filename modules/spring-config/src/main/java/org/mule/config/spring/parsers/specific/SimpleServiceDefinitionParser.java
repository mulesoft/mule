/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.factories.SimpleServiceFactoryBean;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.object.PrototypeObjectFactory;
import org.mule.util.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SimpleServiceDefinitionParser extends AbstractFlowConstructDefinitionParser
{
    private static final String COMPONENT_CLASS_ATTRIBUTE = "component-class";

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return SimpleServiceFactoryBean.class;
    }

    // TODO support @s: endpoint-ref transformer-refs responseTransformer-refs component-ref
    // TODO support child: component
    // TODO support parent inheritance

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        // FIXME replace this with the dynamic addition of a component child
        String componentClassAttribute = element.getAttribute(COMPONENT_CLASS_ATTRIBUTE);
        if (StringUtils.isNotBlank(componentClassAttribute))
        {
            DefaultJavaComponent component = new DefaultJavaComponent(
                new PrototypeObjectFactory(componentClassAttribute));
            component.setEntryPointResolverSet(new LegacyEntryPointResolverSet());
            builder.addPropertyValue("component", component);
        }
        element.removeAttribute(COMPONENT_CLASS_ATTRIBUTE);

        super.doParse(element, parserContext, builder);
    }
}
