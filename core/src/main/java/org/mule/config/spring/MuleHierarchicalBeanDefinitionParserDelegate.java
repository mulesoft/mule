/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This parser enables Mule to parse heirarchical bean structures using spring Namespace handling
 * There are 4 base DefinitionParsers supplied in Mule that Most Parsers will extend from, these are
 * {@link org.mule.config.spring.parsers.AbstractChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.ChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.MuleChildDefinitionParser}
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate
{
    public static final String MULE_DEFAULT_NAMESPACE = "http://www.mulesource.org/schema/mule/core";
    public static final String MULE_NAMESPACE_PREFIX = "http://www.mulesource.org/schema/mule/";    
    
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleHierarchicalBeanDefinitionParserDelegate.class);

    public MuleHierarchicalBeanDefinitionParserDelegate (XmlReaderContext readerContext)
    {
        super(readerContext);
    }

    public BeanDefinition parseCustomElement(Element element, BeanDefinition parent)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("parsing: " + elementToString(element));
        }
        if (delegateSpringElements(element, parent))
        {
            return parent;
        }
        else
        {
            String namespaceUri = element.getNamespaceURI();
            NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler == null)
            {
                getReaderContext().error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", element);
                return null;
            }
            BeanDefinition child = handler.parse(element, new ParserContext(getReaderContext(), this, parent));
            registerBean(element, child);

            // Only iterate and parse child mule name-spaced elements. Spring does not do
            // hierarchical parsing by default so we need to maintain this behavior
            // for non-mule elements to ensure that we don't break the parsing of any
            // other custom name-spaces e.g spring-jee
            if (isMuleNamespace(element))
            {
                NodeList list = element.getChildNodes();
                for (int i = 0; i < list.getLength() ; i++)
                {
                    if(list.item(i) instanceof Element)
                    {
                        parseCustomElement((Element) list.item(i), child);
                    }
                }
            }
            return child;
        }
    }


    protected boolean delegateSpringElements(Element element, BeanDefinition bd)
    {
        if (! isBeansNamespace(element))
        {
            return false;
        }
        
        if (isLocalName(element, PROPERTY_ELEMENT))
        {
            parsePropertyElement(element, bd);
        }
        else if (isLocalName(element, MAP_ELEMENT))
        {
            parseMapElement(element, bd);
        }
        else if (isLocalName(element, LIST_ELEMENT))
        {
            parseListElement(element, bd);
        }
        else if (isLocalName(element, SET_ELEMENT))
        {
            parseSetElement(element, bd);
        }
        else if (isLocalName(element, BEAN_ELEMENT))
        {
            registerBeanDefinitionHolder(parseBeanDefinitionElement(element, bd));
        }
        else if (logger.isWarnEnabled())
        {
            // perhaps should we fail here
            logger.warn("Unexpected Spring element: " + elementToString(element));
        }
        return true;
    }

    public static String elementToString(Element e)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(e.getTagName()).append("{");
        for (int i = 0; i < e.getAttributes().getLength(); i++)
        {
            Node n = e.getAttributes().item(i);
            buf.append(n.getLocalName()).append("=").append(n.getNodeValue()).append(", ");

        }
        buf.append("}");
        return buf.toString();
    }

    protected void registerBean(Element ele, BeanDefinition bd)
    {
        if (bd == null)
        {
            return;
        }
        //Check to see if the Bean Definition represents a compound element - one represents a subset of
        //configuration for the parent bean. Compound bean definitions should not be registered since the properties
        //set on them are really set on the parent bean.
        Boolean compoundElement = (Boolean)bd.getAttribute(ParentDefinitionParser.COMPOUND_ELEMENT);
        if(Boolean.TRUE.equals(compoundElement))
        {
            return;
        }

        String name =  generateChildBeanName(ele);
        registerBeanDefinitionHolder(new BeanDefinitionHolder(bd, name));
    }

    protected void registerBeanDefinitionHolder(BeanDefinitionHolder bdHolder)
    {
        //bdHolder = decorateBeanDefinitionIfRequired(ele, bdHolder);
        // Register the final decorated instance.
        BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
        // Send registration event.
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }

    protected String generateChildBeanName(Element e)
    {
        String id = e.getAttribute("name");
        if (StringUtils.isBlank(id))
        {
            String parentId = ((Element) e.getParentNode()).getAttribute("name");
            id = e.getLocalName();
            return "." + parentId + ":" + id;
        }
        else
        {
            return id;
        }
    }

    public static boolean isMuleNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.startsWith(MULE_NAMESPACE_PREFIX);
    }

    public static boolean isBeansNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.equals(BEANS_NAMESPACE_URI);
    }

    public static boolean isLocalName(Element element, String name)
    {
        return element.getLocalName().equals(name);
    }

}
