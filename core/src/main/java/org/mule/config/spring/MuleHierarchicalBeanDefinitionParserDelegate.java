/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.util.StringUtils;
import org.mule.config.spring.parsers.generic.CompoundElementDefinitionParser;

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
 * {@link org.mule.config.spring.parsers.AbstractChildBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.AbstractMuleSingleBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.SimpleChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.SingleElementDefinitionParser}
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate
{
    public static final String MULE_DEFAULT_NAMESPACE = "http://www.mulesource.org/schema/mule/core";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleHierarchicalBeanDefinitionParserDelegate.class);

    public MuleHierarchicalBeanDefinitionParserDelegate (XmlReaderContext readerContext)
    {
        super(readerContext);
    }


    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd)
    {
        BeanDefinition root;
        BeanDefinition bd = containingBd;

        if (logger.isDebugEnabled()) {
            logger.debug("parsing: " + writeNode(ele));
        }

        //If element is not a Spring property element, use a custom handler
        if(!tryParsingSpringPropertyElements(ele, bd))
        {
            String namespaceUri = ele.getNamespaceURI();
            NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler == null)
            {
                getReaderContext().error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", ele);
                return null;
            }
            bd = handler.parse(ele, new ParserContext(getReaderContext(), this, containingBd));
            registerBean(ele, bd);
        }
        root = bd;

        //Grab all nested elements lised as children to this element
        NodeList list = ele.getChildNodes();
        for (int i = 0; i < list.getLength() ; i++)
        {
            if(list.item(i) instanceof Element)
            {
                Element element = (Element) list.item(i);

                if (logger.isDebugEnabled()) {
                    logger.debug("parsing: " + writeNode(element));
                }
                if (!tryParsingSpringPropertyElements(element, bd))
                {
                    bd = parseCustomElement(element, bd);
                    registerBean(element, bd);
                }
            }
        }
        return root;
    }


    protected boolean tryParsingSpringPropertyElements(Element element, BeanDefinition bd)
    {
        //We add the Spting propertyType to the mule.xsd schema so property elements must match the
        //mule namespace URI for this custom parser to process them
        String ns = (element.getNamespaceURI());
        if (StringUtils.isNotBlank(ns) && ns.startsWith(MULE_DEFAULT_NAMESPACE))
        {
            if (PROPERTY_ELEMENT.equals(element.getLocalName()))
            {
                parsePropertyElement(element, bd);
                return true;
            }
            else if (MAP_ELEMENT.equals(element.getLocalName()))
            {
                parseMapElement(element, bd);
                return true;
            }
            else if (LIST_ELEMENT.equals(element.getLocalName()))
            {
                parseListElement(element, bd);
                return true;
            }
            else if (SET_ELEMENT.equals(element.getLocalName()))
            {
                parseSetElement(element, bd);
                return true;
            }

        }
        //This is a slight hack since if we get a spring namespaced element here it means that we have a Mule element
        //that has a spring element embedded. This is not ideal, and we could just mandate that nested spring properties
        //within Mule elements should always use the mule namespace.
        //Currently only property collections (such as on the <mule:endpoint>) will hit this code
        else if(BEANS_NAMESPACE_URI.equals(ns))
        {
            return true;
        }
        return false;
    }


    private String writeNode(Element e)
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
        Boolean compoundElement = (Boolean)bd.getAttribute(CompoundElementDefinitionParser.COMPOUND_ELEMENT);
        if(Boolean.TRUE.equals(compoundElement))
        {
            return;
        }

        String name =  generateChildBeanName(ele);
        BeanDefinitionHolder bdHolder = new BeanDefinitionHolder(bd, name);

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

}
