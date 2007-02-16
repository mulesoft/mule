/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.w3c.dom.Element;

/**
 * TODO document
 */
public abstract class AbstractNamespaceHandler implements NamespaceHandler
{

    public static final String BASE_DEFINITION_PARSER_LOCATION = "META-INF/services/org/mule/config/";

    /**
     * {@link ClassLoader} instance used to load mapping resources.
     */
    private ClassLoader classLoader;

    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the
     * local name of the {@link Element Elements} they handle.
     */
    private final Map parsers = new HashMap();

    protected AbstractNamespaceHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Element}.
     */
    public final BeanDefinitionParser findParserForElement(Element element) {
        try {
            if(parsers.size()==0) registerBeanDefinitionParsers();
        } catch (ConfigurationException e) {
            throw new FatalBeanException(e.getMessage(), e);
        }

        BeanDefinitionParser parser = (BeanDefinitionParser) this.parsers.get(element.getLocalName());

        if(parser == null) {
            throw new IllegalArgumentException("Cannot locate BeanDefinitionParser for element [" +
                    element.getLocalName() + "].");
        }

        return parser;
    }

    protected void registerBeanDefinitionParsers() throws ConfigurationException {

        try {
            Properties p = loadDefinitionParsers();
            for (Iterator iterator = p.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                BeanDefinitionParser parser = null;
                parser = (BeanDefinitionParser) ClassUtils.instanciateClass(entry.getValue().toString(), ClassUtils.NO_ARGS);
                registerBeanDefinitionParser(entry.getKey().toString(), parser);
            }
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Element}.
     */
    public final BeanDefinitionDecorator findDecoratorForElement(Element element) {
        throw new UnsupportedOperationException("findDecoratorForElement");
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to
     * handle the specified element. The element name is the local (non-namespace qualified)
     * name.
     */
    protected void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
        this.parsers.put(elementName, parser);
    }

    private Properties loadDefinitionParsers() throws ConfigurationException {
        String location = BASE_DEFINITION_PARSER_LOCATION + getElementType() + ".parsers";
        try {
            return PropertiesLoaderUtils.loadAllProperties(location, this.classLoader);
        }
        catch (IOException ex) {
            throw new ConfigurationException(new Message("spring", 2,
                    BASE_DEFINITION_PARSER_LOCATION + getElementType()), ex);
        }
    }

    public abstract String getElementType();

    public static void generateBeanNameIfNotSet(Element e, Class clazz) {
        String id = e.getAttribute("id");
        if(StringUtils.isBlank(id)) {
            id = e.getAttribute("name");
        }
        if(StringUtils.isBlank(id)) id = ClassUtils.getClassName(clazz);
        e.setAttribute("name", id);
    }
}
