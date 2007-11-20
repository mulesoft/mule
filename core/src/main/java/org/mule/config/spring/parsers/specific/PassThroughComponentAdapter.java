/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.components.simple.PassThroughComponent;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.processors.BlockAttribute;
import org.mule.config.spring.parsers.delegate.SingleParentFamilyDefinitionParser;
import org.mule.util.object.AbstractObjectFactory;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Take a parser for a service and add a pass through component as the default.  This can
 * be overridden by subsequent elements.
 */
public class PassThroughComponentAdapter extends SingleParentFamilyDefinitionParser
{

    public PassThroughComponentAdapter(MuleDefinitionParser serviceParser)
    {
        super(serviceParser);

        MuleChildDefinitionParser passThrough = new SimplePojoServiceDefinitionParser(PassThroughComponent.class);
        // ignore attributes from the service
        passThrough.setIgnoredDefault(true);
        // don't call this child if class is defined on the service
        passThrough.registerPreProcessor(new BlockAttribute(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME));
        addHandledException(BlockAttribute.BlockAttributeException.class);
        addChildDelegate(passThrough);
    }

    // for debug intercept
    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return super.parseDelegate(element, parserContext);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
