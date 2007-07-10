/*
 * $Id:TransactionConfigDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.impl.MuleTransactionConfig;
import org.mule.util.ClassUtils;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * This parser is responsible for processing the <code><transaction-config><code> configuration elements.
 */
public class TransactionConfigDefinitionParser extends ChildDefinitionParser
{

    public TransactionConfigDefinitionParser()
    {
        super("transactionConfig", MuleTransactionConfig.class);
        addMapping("action", "NONE=0,ALWAYS_BEGIN=1,BEGIN_OR_JOIN=2,ALWAYS_JOIN=3,JOIN_IF_POSSIBLE=4");
    }

//    //@Override
//    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
//    {
//        super.parseChild(element, parserContext, builder);    //To change body of overridden methods use File | Settings | File Templates.
//    }
//
//    //@Override
//    protected void processProperty(Attr attribute, BeanDefinitionBuilder builder)
//    {
//         if ("factory".equals(attribute.getNodeName()))
//            {
//                String clazz = attribute.getNodeValue();
//                try
//                {
//                    Object o = ClassUtils.instanciateClass(clazz, ClassUtils.NO_ARGS);
//                    builder.addPropertyValue("factory", o);
//                }
//                catch (Exception e)
//                {
//                    throw new BeanCreationException(CoreMessages.cannotLoadFromClasspath(clazz).getMessage(), e);
//                }
//            }
//        else {
//            super.processProperty(attribute, builder);
//         }
//    }
}
