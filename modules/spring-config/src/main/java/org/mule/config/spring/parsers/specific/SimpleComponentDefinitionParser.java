/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.component.DefaultJavaComponent;
import org.mule.object.AbstractObjectFactory;
import org.mule.object.SingletonObjectFactory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Used to parse shortcut elements for simple built-in components such as
 * {@link org.mule.component.simple.EchoComponent} and
 * {@link org.mule.component.simple.LogComponent}. This allows shortcuts like
 * for example <i>&lt;mule:bridge-service/&gt;</i> to be used instead of having to
 * use the <i>&lt;mule:service/&gt;</i> element and specify the class name (and
 * scope) for built-in components that don't require configuration. <p/> <b>This
 * DefinitionParser should only be used for state-less components.</b> <p/> In order
 * to further customize components and use serviceFactory properties the
 * &lt;mule:service/&gt; element should be used.
 */
public class SimpleComponentDefinitionParser extends ComponentDefinitionParser
{
    private static Class OBJECT_FACTORY_TYPE = SingletonObjectFactory.class;
    private Class componentInstanceClass;
    private Map properties = new HashMap();
    
    public SimpleComponentDefinitionParser(Class component, Class componentInstanceClass)
    {
        super(DefaultJavaComponent.class);
        this.componentInstanceClass = componentInstanceClass;
    }

    @Override
    protected void preProcess(Element element)
    {
        super.preProcess(element);

        NamedNodeMap attrs = element.getAttributes();
        
        int numAttrs = attrs.getLength();
        Node attr;
        for (int i = numAttrs-1; i >= 0; --i)
        {
            attr = attrs.item(i);
            if (attr.getNamespaceURI() == null)
            {
                properties.put(attr.getNodeName(), attr.getNodeValue());
                attrs.removeNamedItem(attr.getNodeName());
            }
        }
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        // Create a BeanDefinition for the nested object factory and set it a
        // property value for the component
        builder.addPropertyValue("objectFactory", getObjectFactoryDefinition(element));

        super.parseChild(element, parserContext, builder);
    }
    
    protected AbstractBeanDefinition getObjectFactoryDefinition(Element element)
    {
        AbstractBeanDefinition objectFactoryBeanDefinition = new GenericBeanDefinition();
        objectFactoryBeanDefinition.setBeanClass(OBJECT_FACTORY_TYPE);
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS, componentInstanceClass);
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("properties", properties);

        return objectFactoryBeanDefinition;
    }
}
