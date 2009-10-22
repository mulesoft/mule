/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parser.specific;

import org.mule.config.annotations.Service;
import org.mule.config.spring.factories.ScopedObjectFactory;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.object.AbstractObjectFactory;
import org.mule.util.ClassUtils;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Always creates a {@link org.mule.config.spring.factories.ScopedObjectFactory} component, then when passing
 * the Xml definition, it will set the 'scope' property based on the annotations on the class.
 */
public class AnnotatedPojoComponentDefinitionParser extends ObjectFactoryDefinitionParser
{
    public AnnotatedPojoComponentDefinitionParser()
    {
        super(ScopedObjectFactory.class, "serviceFactory");
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        String serviceName = ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_NAME);
        builder.addDependsOn(serviceName);

        //The next bit of code is superfluous. This is handled in the AnnotatedServicebuilder, but for some reason the
        //ObjectFactory is getting initialised before service object (even though a depenency is set).
        String temp = element.getAttribute(ATTRIBUTE_CLASS);
        Class serviceClass;
        try
        {
            serviceClass = ClassUtils.loadClass(temp, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new BeanCreationException("failed to load annotate bean class", e);
        }
        builder.addPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS, serviceClass);
        getBeanAssembler(element, builder).extendBean(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS, serviceClass, false);


        if (serviceClass.isAnnotationPresent(Service.class))
        {
            Service service = (Service) serviceClass.getAnnotation(Service.class);
            builder.addPropertyValue("scope", service.scope());
        }
    }
}