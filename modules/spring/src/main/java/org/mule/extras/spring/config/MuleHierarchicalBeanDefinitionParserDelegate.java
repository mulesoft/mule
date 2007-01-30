/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * TODO
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate
{

    public MuleHierarchicalBeanDefinitionParserDelegate (XmlReaderContext readerContext)
    {
        super(readerContext);
    }


    public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd)
    {

        BeanDefinition root;
        String namespaceUri = ele.getNamespaceURI();
        NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null)
        {
          //  if (isDefaultNamespace(namespaceUri))
          //  {
          //      if(PROPERTY_ELEMENT.equals(ele.getLocalName()))
          //      {
          //          parsePropertyElements(ele, containingBd);
                    //return null;
           //     }
//                else if(PROPERTY_ELEMENT.equals(ele.getParentNode().getLocalName()))
//                {
//                   parsePropertySubElement(ele, containingBd);
//                    return null;
//                }
//                else
//                {
//                    BeanDefinitionHolder bdh = parseBeanDefinitionElement(ele, containingBd);
//                    if (bdh != null)
//                    {
//                        return bdh.getBeanDefinition();
//                    }
//                    else
//                    {
//                        return null;
//                    }
//                }
            //    return null;
            //}
            getReaderContext().error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", ele);
            return null;
        }
        BeanDefinition bd = handler.parse(ele, new ParserContext(getReaderContext(), this, containingBd));
        registerBean(ele, bd);
        root = bd;
        //Grab all nested elements lised as children to this element
        NodeList list = ele.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < list.getLength(); i++)
        {
            Element element = (Element) list.item(i);
            System.out.println(element.toString());
            if (isDefaultNamespace(element.getNamespaceURI()) && (PROPERTY_ELEMENT.equals(element.getLocalName())))
            {
                parsePropertyElements(element, bd);
                break;
            }
            else
            {
                bd = parseCustomElement(element, bd);
                //registerBean(element, bd);
            }
        }
        return root;
    }

    protected void registerBean(Element ele, BeanDefinition bd)
    {
        if (bd == null)
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
        String parentId = ((Element) e.getParentNode()).getAttribute("id");
        //String parentBean = e.getLocalName() + ":" + ((Element) e.getParentNode()).getAttribute("id");
        String id = e.getAttribute("id");
        if (StringUtils.isBlank(id))
        {
            id = e.getLocalName();
            return "." + parentId + ":" + id;
        }
        else
        {
            return id;
        }

    }

}
