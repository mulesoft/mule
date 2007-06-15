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

import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * This parser extends the Spring provided {@link AbstractBeanDefinitionParser} to provide additional features for
 * consistently customising bean representations for Mule bean definition parsers.  Most custom bean definition parsers
 * in Mule will use this base class. The following enhancements are made -
 *
 * 1. A property name which ends with the suffix "-ref" is assumed to be a reference to another bean.
 * Alternatively, a property can be explicitly registered as a bean reference via registerBeanReference()
 *
 * For example,
 *
 *     <code> &lt;bpm:connector bpms-ref=&quot;testBpms&quot;/&lt; </code>
 *
 * will automatically set a property "bpms" on the connector to reference a bean named "testBpms"
 *
 * 2. Attribute mappings can be registered to control how an attribute name in Mule Xml maps to the bean name in the
 * object being created. For example -
 *
 * <code>registerAttributeMapping("poolExhaustedAction", "poolExhaustedActionString");</code>
 *
 * Map the 'poolExhaustedAction' to the 'poolExhaustedActionString' property on the bean being created.
 *
 * 3. Value Mappings can be used to map key value pairs from selection lists in the XML schema to property values on the
 * bean being created. These are a comma-separated list of key=value pairs. For example -
 *
 *     <code> registerValueMapping("action", "NONE=0,ALWAYS_BEGIN=1,BEGIN_OR_JOIN=2,JOIN_IF_POSSIBLE=3");</code>
 *
 * The first argument is the bean name to set, the second argument is the set of possible key=value pairs
 *
 * 4. Provides an automatic way of setting the 'init-method' and 'destroy-method' for this object. This will then automatically
 * wire the bean into the lifecycle of the Application context.
 *
 * 5. The 'singleton' property provides a fixed way to make sure the bean is always a singleton or not.
 *
 * @see  AbstractBeanDefinitionParser
 */
public abstract class AbstractMuleSingleBeanDefinitionParser extends AbstractBeanDefinitionParser
{
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_IDREF = "nameref";
    public static final String ATTRIBUTE_CLASS = "class";
    public static final String ATTRIBUTE_REF_SUFFIX = "-ref";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected PropertyToolkit propertyToolkit = new PropertyToolkit();
    protected Properties attributeMappings;
    protected Map valueMappings;
    protected List beanReferences;
    protected ParserContext parserContext;
    //By default Mule objects are not singletons
    protected boolean singleton = false;
    /** Allow the bean class to be set explicitly via the "class" attribute. */
    protected boolean allowClassAttribute = true;


    public void registerBeanReference(String propertyName)
    {
        propertyToolkit.registerBeanReference(propertyName);
    }

    public void registerValueMapping(PropertyToolkit.ValueMap mapping)
    {
        propertyToolkit.registerValueMapping(mapping);
    }

    public void registerValueMapping(String propertyName, Map mappings)
    {
        propertyToolkit.registerValueMapping(propertyName, mappings);
    }

    public void registerValueMapping(String propertyName, String mappings)
    {
        propertyToolkit.registerValueMapping(propertyName, mappings);
    }

    public void registerAttributeMapping(String alias, String propertyName)
    {
        propertyToolkit.registerAttributeMapping(alias, propertyName);
    }

    public void registerCollection(String propertyName)
    {
        propertyToolkit.registerCollection(propertyName);
    }

    public void registerIgnored(String propertyName)
    {
        propertyToolkit.registerIgnored(propertyName);
    }

    protected void processProperty(Attr attribute, BeanDefinitionBuilder builder)
    {
        boolean isBeanReference = propertyToolkit.isBeanReference(attribute.getNodeName());
        String propertyName = propertyToolkit.extractPropertyName(attribute.getNodeName());
        String propertyValue = propertyToolkit.extractPropertyValue(propertyName, attribute.getValue());
        Assert.state(StringUtils.hasText(propertyName),
                "Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
        addProperty(builder, propertyName, propertyValue, isBeanReference);
    }

    protected void addProperty(BeanDefinitionBuilder builder, String name, String value, boolean reference)
    {
        if (!propertyToolkit.isIgnored(name))
        {
            // The property may be a reference to another bean.
            if (reference)
            {
                builder.addPropertyReference(name, value);
            }
            else
            {
                builder.addPropertyValue(name, value);
            }
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

    /**
     * Hook method that derived classes can implement to modify internal state before processing.
     * <p>The default implementation does nothing.
     */
    protected void preProcess()
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
        preProcess();
        Class beanClass = null;
        if (allowClassAttribute)
        {
            beanClass = getBeanClassFromAttribute(element);
        }
        if (beanClass == null)
        {
            beanClass = getBeanClass(element);
        }
        Assert.state(beanClass != null, "Class returned from getBeanClass(Element) must not be null, element is: " + element.getNodeName());
        BeanDefinitionBuilder builder = createBeanDefinitionBuilder(element, beanClass);
        builder.setSource(parserContext.extractSource(element));
        builder.setSingleton(isSingleton());
        builder.addDependsOn("_muleRegistry");

        List interfaces = ClassUtils.getAllInterfaces(beanClass);
        if(interfaces!=null)
        {
            if(interfaces.contains(Initialisable.class))
            {
                builder.setInitMethodName(Initialisable.PHASE_NAME);
            }

            if(interfaces.contains(Disposable.class))
            {
                builder.setDestroyMethodName(Disposable.PHASE_NAME);
            }
        }

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
     * Determine the bean class corresponding to the supplied {@link Element} based on an
     * explicit "class" attribute.
     *
     * @param element the <code>Element</code> that is being parsed
     * @return the {@link Class} of the bean that is being defined via parsing the supplied <code>Element</code>
     *         (must <b>not</b> be <code>null</code>)
     * @see #parseInternal(org.w3c.dom.Element,ParserContext)
     */
    protected Class getBeanClassFromAttribute(Element element)
    {
        String className = element.getAttribute(ATTRIBUTE_CLASS);
        Class clazz = null;
        if (org.mule.util.StringUtils.isNotBlank(className))
        {
            try
            {
                element.removeAttribute(ATTRIBUTE_CLASS);
                //RM* Todo probably need to use OSGi Loader here
                clazz = ClassUtils.loadClass(className, getClass());
            }
            catch (ClassNotFoundException e)
            {
                logger.error("could not load class: " + className, e);
            }
        }
        return clazz;
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

    protected boolean isSingleton()
    {
        return singleton;
    }

}
