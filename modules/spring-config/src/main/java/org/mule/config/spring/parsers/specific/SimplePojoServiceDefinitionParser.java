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

import org.mule.util.object.AbstractObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Used to parse shortcut elements for simple built-in components such as
 * {@link org.mule.component.simple.BridgeComponent},
 * {@link import org.mule.component.simple.EchoComponent} and
 * {@link import org.mule.component.simple.LogComponent}. This allows shortcuts
 * like for example <i>&lt;mule:bridge-service/&gt;</i> to be used instead of
 * having to use the <i>&lt;mule:service/&gt;</i> element and specify the class
 * name (and scope) for built-in components that don't require configuration. <p/>
 * <b>This DefinitionParser should only be used for state-less components.</b> <p/>
 * In order to further customize components and use serviceFactory properties the
 * &lt;mule:service/&gt; element should be used.
 */
public class SimplePojoServiceDefinitionParser extends ObjectFactoryDefinitionParser
{
    private Class clazz;

    public SimplePojoServiceDefinitionParser(Class clazz)
    {
        this(clazz, "componentFactory");
    }

    public SimplePojoServiceDefinitionParser(Class clazz, String setter)
    {
        super(SingletonObjectFactory.class, setter);
        this.clazz = clazz;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        getBeanAssembler(element, builder).extendBean(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS, clazz, false);
        super.parseChild(element, parserContext, builder);
    }
}
