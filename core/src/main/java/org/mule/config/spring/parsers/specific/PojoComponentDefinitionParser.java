/*
 * $Id: ObjectFactoryDefinitionParser.java 8083 2007-08-28 02:25:36Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.impl.UMOComponentAware;
import org.mule.util.ClassUtils;
import org.mule.util.object.AbstractObjectFactory;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class PojoComponentDefinitionParser extends ObjectFactoryDefinitionParser
{

    public PojoComponentDefinitionParser(Class beanClass)
    {
        this(beanClass, "serviceFactory");
    }                                                             
    
    public PojoComponentDefinitionParser(Class beanClass, String setterMethod)
    {
        super(beanClass, setterMethod);
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);

        // Get the POJO's class.
        MutablePropertyValues beanProperties = builder.getBeanDefinition().getPropertyValues();
        Class objectClass = null;
        if (beanProperties.getPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS) != null)
        {
            objectClass = (Class) beanProperties.getPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS).getValue();
        }
        if (objectClass == null)
        {
            if (beanProperties.getPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME) != null)
            {
                String objectClassName = (String) beanProperties.getPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME).getValue();
                try
                {
                    objectClass = ClassUtils.getClass(objectClassName);
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }            

        // Inject the UMOComponent into the POJO if the POJO needs it.
        if (objectClass != null && UMOComponentAware.class.isAssignableFrom(objectClass))
        {
            logger.debug("Injecting UMOComponent into class " + objectClass + " which implements the UMOComponentAware interface.");
            // The UMOComponent should theoretically be the parent node.
            Element parent = (Element) element.getParentNode();
            String componentName = parent.getAttribute(ATTRIBUTE_NAME);
            builder.addPropertyReference("component", componentName);
        }
    }
}
