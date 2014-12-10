/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.StringUtils;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * This parser enables Mule to parse heirarchical bean structures using spring Namespace handling
 * There are 4 base DefinitionParsers supplied in Mule that most Parsers will extend from, these are
 * {@link org.mule.config.spring.parsers.AbstractChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.ChildDefinitionParser}
 * {@link org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser}
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate
{

    public static final String BEANS = "beans"; // cannot find this in Spring api!
    public static final String MULE_REPEAT_PARSE = "org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE";
    public static final String MULE_NO_RECURSE = "org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE";
    public static final String MULE_FORCE_RECURSE = "org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_FORCE_RECURSE";
    public static final String MULE_NO_REGISTRATION = "org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_REGISTRATION";
    public static final String MULE_POST_CHILDREN = "org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_POST_CHILDREN";
    private DefaultBeanDefinitionDocumentReader spring;
    private final List<ElementValidator> elementValidators;

    protected static final Log logger = LogFactory.getLog(MuleHierarchicalBeanDefinitionParserDelegate.class);

    public MuleHierarchicalBeanDefinitionParserDelegate(XmlReaderContext readerContext,
                                                        DefaultBeanDefinitionDocumentReader spring, ElementValidator... elementValidators)
    {
        super(readerContext);
        this.spring = spring;
        this.elementValidators = ArrayUtils.isEmpty(elementValidators) ? ImmutableList.<ElementValidator>of() : ImmutableList.copyOf(elementValidators);
    }

    public BeanDefinition parseCustomElement(Element element, BeanDefinition parent)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("parsing: " + SpringXMLUtils.elementToString(element));
        }

        validate(element);

        if (SpringXMLUtils.isBeansNamespace(element))
        {
            return handleSpringElements(element, parent);
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

            boolean noRecurse = false;
            boolean forceRecurse = false;
            BeanDefinition finalChild;

            do {
                ParserContext parserContext = new ParserContext(getReaderContext(), this, parent);
                finalChild = handler.parse(element, parserContext);
                registerBean(element, finalChild);
                noRecurse = noRecurse || testFlag(finalChild, MULE_NO_RECURSE);
                forceRecurse = forceRecurse || testFlag(finalChild, MULE_FORCE_RECURSE);
            } while (null != finalChild && testFlag(finalChild, MULE_REPEAT_PARSE));

            // Only iterate and parse child mule name-spaced elements. Spring does not do
            // hierarchical parsing by default so we need to maintain this behavior
            // for non-mule elements to ensure that we don't break the parsing of any
            // other custom name-spaces e.g spring-jee.

            // We also avoid parsing inside elements that have constructed a factory bean
            // because that means we're dealing with (something like) ChildMapDefinitionParser,
            // which handles iteration internally (this is a hack needed because Spring doesn't
            // expose the DP for "<spring:entry>" elements directly).

            boolean isRecurse;
            if (noRecurse)
            {
                // no recursion takes precedence, as recursion is set by default
                isRecurse = false;
            }
            else
            {
                if (forceRecurse)
                {
                    isRecurse = true;
                }
                else
                {
                    // default behaviour if no control specified
                    isRecurse = SpringXMLUtils.isMuleNamespace(element);
                }
            }

            if (isRecurse)
            {
                NodeList list = element.getChildNodes();
                for (int i = 0; i < list.getLength(); i++)
                {
                    if (list.item(i) instanceof Element)
                    {
                        parseCustomElement((Element) list.item(i), finalChild);
                    }
                }
            }

            // If a parser requests post-processing we call again after children called

            if (testFlag(finalChild, MULE_POST_CHILDREN))
            {
                ParserContext parserContext = new ParserContext(getReaderContext(), this, parent);
                finalChild = handler.parse(element, parserContext);
            }

            return finalChild;
        }
    }

    private void validate(Element element)
    {
        for (ElementValidator validator : elementValidators)
        {
            validator.validate(element);
        }
    }

    protected BeanDefinition handleSpringElements(Element element, BeanDefinition parent)
    {

        // these are only called if they are at a "top level" - if they are nested inside
        // other spring elements then spring will handle them itself

        if (SpringXMLUtils.isLocalName(element, BEANS))
        {
            // the delegate doesn't support the full spring schema, but it seems that
            // we can invoke the DefaultBeanDefinitionDocumentReader via registerBeanDefinitions
            // but we need to create a new DOM document from the element first
            try
            {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                doc.appendChild(doc.importNode(element, true));
                spring.registerBeanDefinitions(doc, getReaderContext());
            }
            catch (ParserConfigurationException e)
            {
                throw new RuntimeException(e);
            }
            return parent;
        }

        else if (SpringXMLUtils.isLocalName(element, PROPERTY_ELEMENT))
        {
            parsePropertyElement(element, parent);
            return parent;
        }

        // i am trying to keep these to a minimum - using anything but "bean" is a recipe
        // for disaster - we already have problems with "property", for example.

//        else if (isLocalName(element, MAP_ELEMENT))
//        {
//            // currently unused?
//            parseMapElement(element, bd);
//        }
//        else if (isLocalName(element, LIST_ELEMENT))
//        {
//            // currently unused?
//            parseListElement(element, bd);
//        }
//        else if (isLocalName(element, SET_ELEMENT))
//        {
//            // currently unused?
//            parseSetElement(element, bd);
//        }

        else if (SpringXMLUtils.isLocalName(element, BEAN_ELEMENT))
        {
            BeanDefinitionHolder holder = parseBeanDefinitionElement(element, parent);
            registerBeanDefinitionHolder(holder);
            return holder.getBeanDefinition();
        }
        else
        {
            throw new IllegalStateException("Unexpected Spring element: " + SpringXMLUtils.elementToString(element));
        }
    }

    protected void registerBean(Element ele, BeanDefinition bd)
    {
        if (bd == null)
        {
            return;
        }

        // Check to see if the Bean Definition represents a compound element - one represents a subset of
        // configuration for the parent bean. Compound bean definitions should not be registered since the properties
        // set on them are really set on the parent bean.
        if (! testFlag(bd, MULE_NO_REGISTRATION))
        {
            String name =  generateChildBeanName(ele);
            logger.debug("register " + name + ": " + bd.getBeanClassName());
            registerBeanDefinitionHolder(new BeanDefinitionHolder(bd, name));
        }
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
        String id = SpringXMLUtils.getNameOrId(e);
        if (StringUtils.isBlank(id))
        {
            String parentId = SpringXMLUtils.getNameOrId((Element) e.getParentNode());
            return "." + parentId + ":" + e.getLocalName();
        }
        else
        {
            return id;
        }
    }

    public static void setFlag(BeanDefinition bean, String flag)
    {
        bean.setAttribute(flag, Boolean.TRUE);
    }

    public static boolean testFlag(BeanDefinition bean, String flag)
    {
        return null != bean
                && bean.hasAttribute(flag)
                && bean.getAttribute(flag) instanceof Boolean
                && ((Boolean) bean.getAttribute(flag)).booleanValue();
    }


    /**
     * Parse a map element.
     */
    public Map parseMapElement(Element mapEle, String mapElementTagName, String mapElementKeyAttributeName, String mapElementValueAttributeName) {
        List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, mapElementTagName);
        ManagedMap<Object, Object> map = new ManagedMap<Object, Object>(entryEles.size());
        map.setSource(extractSource(mapEle));
        map.setMergeEnabled(parseMergeAttribute(mapEle));

        for (Element entryEle : entryEles) {
            // Extract key from attribute or sub-element.
            Object key = buildTypedStringValueForMap(entryEle.getAttribute(mapElementKeyAttributeName), null, entryEle);
            // Extract value from attribute or sub-element.
            Object value = buildTypedStringValueForMap(entryEle.getAttribute(mapElementValueAttributeName), null, entryEle);
            // Add final key and value to the Map.
            map.put(key, value);
        }
        return map;
    }

}
