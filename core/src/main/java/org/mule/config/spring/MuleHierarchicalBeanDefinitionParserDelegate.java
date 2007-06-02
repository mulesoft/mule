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

import org.mule.config.spring.parsers.CompoundElementDefinitionParser;
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
 * {@link org.mule.config.spring.parsers.AbstractChildBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.AbstractMuleSingleBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.SimpleChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.SingleElementDefinitionParser}
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate
{
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
        if (logger.isDebugEnabled()) {
            logger.debug("parsing: " + writeNode(ele));
        }
        BeanDefinition root;
        String namespaceUri = ele.getNamespaceURI();
        NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null)
        {
            getReaderContext().error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", ele);
            return null;
        }
        BeanDefinition bd = handler.parse(ele, new ParserContext(getReaderContext(), this, containingBd));
        registerBean(ele, bd);

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
                if (isDefaultNamespace(element.getNamespaceURI()))
                {
                    if(PROPERTY_ELEMENT.equals(element.getLocalName()))
                    {
                        parsePropertyElement(element, root);
                    }
                    else if(MAP_ELEMENT.equals(element.getLocalName()))
                    {
                        parseMapElement(element, root);
                    }
                    else if(LIST_ELEMENT.equals(element.getLocalName()))
                    {
                        parseListElement(element, root);
                    }
                    else if(SET_ELEMENT.equals(element.getLocalName()))
                    {
                        parseSetElement(element, root);
                    } 
                }
                else
                {
                    bd = parseCustomElement(element, bd);
                    registerBean(element, bd);
                }
            }
        }
        return root;
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
        //configuration for the partent bean. Compound bean definitions should not be registered since the properties
        //set on them are really set on the partent bean.
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
