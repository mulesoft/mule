/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.AbstractParallelDelegatingDefinitionParser;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Allows for parsing either a shortcut component configuration by delegating to two
 * different component parses depending on the existence of the class attribute. If
 * the class attribute is used then an embedded object factory element cannot be
 * used.
 * 
 * <pre>
 * &lt;component class=&quot;&quot;&gt;
 * </pre>
 * 
 * or one with an embedded object factory element.
 * 
 * <pre>
 * &lt;component&gt;
 *     &lt;singleon-object class=&quot;..&quot;/&gt;
 * &lt;/component&gt;
 * </pre>
 */
public class ComponentDelegatingDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    private MuleDefinitionParser normalConfig;
    private MuleDefinitionParser shortcutConfig;

    public ComponentDelegatingDefinitionParser(Class clazz)
    {
        normalConfig = new ComponentDefinitionParser(clazz);
        shortcutConfig = new ShortcutComponentDefinitionParser(clazz);
        addDelegate(normalConfig);
        addDelegate(shortcutConfig);
        registerPreProcessor(new CheckExclusiveClassAttributeObjectFactory());
    }

    @Override
    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        if (StringUtils.isEmpty(element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS)))
        {
            return normalConfig;
        }
        else
        {
            return shortcutConfig;
        }
    }

    /**
     * Given that the service object-factory is extensible and new object factory
     * types can be implemented and used by substitution, the only way of checking
     * for the existence of an object-factory if by object factory element
     * convention.<br>
     * This pre-processor checks for the existence of a <i>"class"</i> attribute on
     * the service, and throws an exception if the service has any elements that
     * match the object factory element convention (i.e. that end in "object"). NOTE:
     * We used to test by exclusion here allowing all other elements, but that no
     * longer works now extensible interceptors elements can be used.
     */
    class CheckExclusiveClassAttributeObjectFactory implements PreProcessor
    {

        private static final String OBJECT_FACTORY_ELEMENT_CONVENTION_SUFFIX = "object";

        public void preProcess(PropertyConfiguration config, Element element)
        {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
                if (alias.equals(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS))
                {
                    for (int j = 0; j < element.getChildNodes().getLength(); j++)
                    {
                        Node child = element.getChildNodes().item(j);
                        if (child instanceof Element
                            && child.getLocalName().endsWith(OBJECT_FACTORY_ELEMENT_CONVENTION_SUFFIX))
                        {
                            StringBuilder message = new StringBuilder("The child element '");
                            message.append(child.getLocalName());
                            message.append("' cannot appear with the 'class' attribute");
                            message.append(" in element ");
                            message.append(SpringXMLUtils.elementToString(element));
                            message.append(".");
                            throw new CheckExclusiveClassAttributeObjectFactoryException(message.toString());
                        }
                    }
                }
            }
        }
    }

    class CheckExclusiveClassAttributeObjectFactoryException extends IllegalStateException
    {
        private static final long serialVersionUID = 4625276914151932111L;

        CheckExclusiveClassAttributeObjectFactoryException(String message)
        {
            super(message);
        }
    }

}
