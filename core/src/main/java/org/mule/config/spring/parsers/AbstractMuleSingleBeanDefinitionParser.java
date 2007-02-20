/*
 * $Id:AbstractMuleSingleBeanDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * TODO document
 */
public abstract class AbstractMuleSingleBeanDefinitionParser extends AbstractBeanDefinitionParser
{
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_IDREF = "nameref";
    public static final String ATTRIBUTE_CLASS = "class";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    //public static final String ID_ATTRIBUTE = "id";
    //public static final String LOCAL_NAMESPACE = "http://mule.mulesource.org/schema/";

    protected Properties attributeMappings;
    protected Map valueMappings;
    protected ParserContext parserContext;

    protected AbstractMuleSingleBeanDefinitionParser()
         {
             attributeMappings = new Properties();
             valueMappings = new HashMap();
         }

    public void registerValueMapping(ValueMap mapping)
    {
        valueMappings.put(mapping.getPropertyName(), mapping);
    }

    public void registerValueMapping(String propertyName, Map mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    public void registerValueMapping(String propertyName, String mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    protected void registerAttributeMapping(String alias, String propertyName)
    {
        attributeMappings.put(alias, propertyName);
    }

    protected String getAttributeMapping(String alias)
    {
        return attributeMappings.getProperty(alias, alias);
    }

    protected void processProperty(Attr attribute, BeanDefinitionBuilder builder)
    {
        String propertyName = extractPropertyName(attribute.getNodeName());
            String propertyValue = extractPropertyValue(propertyName, attribute.getValue());
            Assert.state(StringUtils.hasText(propertyName),
                    "Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
            builder.addPropertyValue(propertyName, propertyValue);
    }

    /**
     * Extract a JavaBean property name from the supplied attribute name.
     * <p>The default implementation uses the {@link org.springframework.core.Conventions#attributeNameToPropertyName(String)}
     * method to perform the extraction.
     * <p>The name returned must obey the standard JavaBean property name
     * conventions. For example for a class with a setter method
     * '<code>setBingoHallFavourite(String)</code>', the name returned had
     * better be '<code>bingoHallFavourite</code>' (with that exact casing).
     *
     * @param attributeName the attribute name taken straight from the XML element being parsed; will never be <code>null</code>
     * @return the extracted JavaBean property name; must never be <code>null</code>
     */
    protected String extractPropertyName(String attributeName)
    {
        attributeName = getAttributeMapping(attributeName);
        return Conventions.attributeNameToPropertyName(attributeName);
    }

    protected String extractPropertyValue(String attributeName, String attributeValue)
    {
        ValueMap vm = (ValueMap)valueMappings.get(attributeName);
        if(vm!=null)
        {
            return vm.getValue(attributeValue).toString();
        }
        else {
            return attributeValue;
        }
    }

    /**
     * Hook method that derived classes can implement to inspect/change a
     * bean definition after parsing is complete.
     * <p>The default implementation does nothing.
     *
     * @param beanDefinition the parsed (and probably totally defined) bean definition being built
     * @param element        the XML element that was the source of the bean definition's metadata
     */
    protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element)
    {
    }

    //-----------------------------------------------------------------------------------
    //- Taken from AbstractSingleBeanDefinitionParser to allow pluggable BeanDefinitionParser
    //-----------------------------------------------------------------------------------

    /**
     * Creates a {@link BeanDefinitionBuilder} instance for the
     * {@link #getBeanClass bean Class} and passes it to the
     * {@link #doParse} strategy method.
     *
     * @param element       the element that is to be parsed into a single BeanDefinition
     * @param parserContext the object encapsulating the current state of the parsing process
     * @return the BeanDefinition resulting from the parsing of the supplied {@link Element}
     * @throws IllegalStateException if the bean {@link Class} returned from
     *                               {@link #getBeanClass(org.w3c.dom.Element)} is <code>null</code>
     * @see #doParse
     */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        this.parserContext = parserContext;
        Class beanClass = getBeanClass(element);
        Assert.state(beanClass != null, "Class returned from getBeanClass(Element) must not be null, element is: " + element.getNodeName());
        BeanDefinitionBuilder builder = createBeanDefinitionBuilder(element, beanClass);
        builder.setSource(parserContext.extractSource(element));
        if (parserContext.isNested())
        {
            // Inner bean definition must receive same singleton status as containing bean.
            builder.setSingleton(parserContext.getContainingBeanDefinition().isSingleton());
        }
        doParse(element, parserContext, builder);
        return builder.getBeanDefinition();
    }

    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        return BeanDefinitionBuilder.rootBeanDefinition(beanClass);
    }

    /**
     * Determine the bean class corresponding to the supplied {@link Element}.
     *
     * @param element the <code>Element</code> that is being parsed
     * @return the {@link Class} of the bean that is being defined via parsing the supplied <code>Element</code>
     *         (must <b>not</b> be <code>null</code>)
     * @see #parseInternal(org.w3c.dom.Element,ParserContext)
     */
    protected abstract Class getBeanClass(Element element);

    /**
     * Parse the supplied {@link Element} and populate the supplied
     * {@link BeanDefinitionBuilder} as required.
     * <p>The default implementation delegates to the <code>doParse</code>
     * version without ParserContext argument.
     *
     * @param element       the XML element being parsed
     * @param parserContext the object encapsulating the current state of the parsing process
     * @param builder       used to define the <code>BeanDefinition</code>
     */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            String name = attribute.getLocalName();
            //If we set an attribute manually, we pick up the name using getName()
            if (name == null)
            {
                name = attribute.getNodeName();
            }

            if (ATTRIBUTE_ID.equals(name) || ATTRIBUTE_IDREF.equals(name))
            {
                continue;
            }

            processProperty(attribute, builder);
        }
        postProcess(builder, element);
    }


    //@Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        String name = element.getAttribute(ATTRIBUTE_NAME);
        if(StringUtils.hasText(name))
        {
            return name;
        }
        return super.resolveId(element, definition, parserContext);
    }

    public static class ValueMap
    {
        private String propertyName;
        private Map mappings;


        public ValueMap(String propertyName, String mappingsString)
        {
            this.propertyName = propertyName;
            mappings = new HashMap();

            String[] values = StringUtils.tokenizeToStringArray(mappingsString, ",");
            for (int i = 0; i < values.length; i++)
            {
                String value = values[i];
                int x = value.indexOf("=");
                if(x==-1)
                {
                    throw new IllegalArgumentException("Mappings string not properly defined: " + mappingsString);
                }
                mappings.put(value.substring(0, x), value.substring(x+1));
            }

        }


        public ValueMap(String propertyName, Map mappings)
        {
            this.propertyName = propertyName;
            this.mappings = mappings;
        }

        public String getPropertyName()
        {
            return propertyName;
        }

        public Object getValue(Object key)
        {
            return mappings.get(key);
        }
    }
}