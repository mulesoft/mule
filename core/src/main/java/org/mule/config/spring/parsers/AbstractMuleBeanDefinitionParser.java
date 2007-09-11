/*
 * $Id:AbstractMuleBeanDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.ReusablePropertyConfiguration;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.ClassUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * This parser extends the Spring provided {@link AbstractBeanDefinitionParser} to provide additional features for
 * consistently customising bean representations for Mule bean definition parsers.  Most custom bean definition parsers
 * in Mule will use this base class. The following enhancements are made -
 *
 * <ol>
 * <li>A property name which ends with the suffix "-ref" is assumed to be a reference to another bean.
 * Alternatively, a property can be explicitly registered as a bean reference via registerBeanReference()
 *
 * <p>For example,
 * <code> &lt;bpm:connector bpms-ref=&quot;testBpms&quot;/&lt; </code>
 * will automatically set a property "bpms" on the connector to reference a bean named "testBpms"
 * </p></li>
 *
 * <li>Attribute mappings can be registered to control how an attribute name in Mule Xml maps to the bean name in the
 * object being created.
 *
 * <p>For example -
 * <code>addAlias("poolExhaustedAction", "poolExhaustedActionString");</code>
 * Maps the 'poolExhaustedAction' to the 'poolExhaustedActionString' property on the bean being created.
 * </p></li>
 *
 * <li>Value Mappings can be used to map key value pairs from selection lists in the XML schema to property values on the
 * bean being created. These are a comma-separated list of key=value pairs.
 *
 * <p>For example -
 * <code>addMapping("action", "NONE=0,ALWAYS_BEGIN=1,BEGIN_OR_JOIN=2,JOIN_IF_POSSIBLE=3");</code>
 * The first argument is the bean name to set, the second argument is the set of possible key=value pairs
 * </p></li>
 *
 * <li>Provides an automatic way of setting the 'init-method' and 'destroy-method' for this object. This will then automatically
 * wire the bean into the lifecycle of the Application context.</li>
 *
 * <li>The 'singleton' property provides a fixed way to make sure the bean is always a singleton or not.</li>
 *
 * <li>Collections will be automatically created and extended if the setter matches "property+s".</li>
 * </ol>
 *
 * <p>Note that this class is not multi-thread safe.  The internal state is reset before each "use"
 * by {@link #preProcess(org.w3c.dom.Element)} which assumes sequential access.</p>
 *
 * @see  AbstractBeanDefinitionParser
 */
public abstract class AbstractMuleBeanDefinitionParser extends AbstractBeanDefinitionParser
    implements MuleDefinitionParser
{
    public static final String ROOT_ELEMENT = "mule";
    public static final String ROOT_UNSAFE_ELEMENT = "mule-unsafe";
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_CLASS = "class";
    public static final String ATTRIBUTE_REF = "ref";
    public static final String ATTRIBUTE_REF_SUFFIX = "-" + ATTRIBUTE_REF;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected BeanAssemblerFactory beanAssemblerFactory = new DefaultBeanAssemblerFactory();
    protected ReusablePropertyConfiguration propertyConfiguration = new ReusablePropertyConfiguration();
    private ParserContext parserContext;
    private BeanDefinitionRegistry registry;
    private LinkedList preProcessors = new LinkedList();
    private List postProcessors = new LinkedList();
    //By default Mule objects are not singletons
    protected boolean singleton = false;

    /** Allow the bean class to be set explicitly via the "class" attribute. */
    private boolean allowClassAttribute = true;
    private Class classConstraint = null;

    public AbstractMuleBeanDefinitionParser()
    {
        addIgnored(ATTRIBUTE_ID);
    }

    public MuleDefinitionParser addReference(String propertyName)
    {
        propertyConfiguration.addReference(propertyName);
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, Map mappings)
    {
        propertyConfiguration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, String mappings)
    {
        propertyConfiguration.addMapping(propertyName, mappings);
        return this;
    }

    /**
     * @param alias The attribute name
     * @param propertyName The bean property name
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    public MuleDefinitionParser addAlias(String alias, String propertyName)
    {
        propertyConfiguration.addAlias(alias, propertyName);
        return this;
    }

    /**
     * @param propertyName Property that is a collection
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    public MuleDefinitionParser addCollection(String propertyName)
    {
        propertyConfiguration.addCollection(propertyName);
        return this;
    }

    /**
     * @param propertyName Property that is to be ignored
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    public MuleDefinitionParser addIgnored(String propertyName)
    {
        propertyConfiguration.addIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParser removeIgnored(String propertyName)
    {
        propertyConfiguration.removeIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParser setIgnoredDefault(boolean ignoreAll)
    {
        propertyConfiguration.setIgnoredDefault(ignoreAll);
        return this;
    }

    protected void processProperty(Attr attribute, BeanAssembler assembler)
    {
        assembler.extendBean(attribute);
    }

    /**
     * Hook method that derived classes can implement to inspect/change a
     * bean definition after parsing is complete.
     * <p>The default implementation does nothing.
     *
     * @param assembler the parsed (and probably totally defined) bean definition being built
     * @param element   the XML element that was the source of the bean definition's metadata
     */
    protected void postProcess(BeanAssembler assembler, Element element)
    {
        AutoIdUtils.ensureUniqueName(element, "bean");
        Iterator processes = postProcessors.iterator();
        while (processes.hasNext())
        {
            ((PostProcessor) processes.next()).postProcess(assembler, element);
        }
    }

    /**
     * Hook method that derived classes can implement to modify internal state before processing.
     *
     * Here we make sure that the internal property configuration state is reset to the
     * initial configuration for each element (it may be modified by the BeanAssembler)
     * and that other mutable instance variables are cleared.
     */
    protected void preProcess(Element element)
    {
        parserContext = null;
        registry = null;
        propertyConfiguration.reset();
        Iterator processes = preProcessors.iterator();
        while (processes.hasNext())
        {
            ((PreProcessor) processes.next()).preProcess(propertyConfiguration, element);
        }
    }

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
        preProcess(element);
        setParserContext(parserContext);
        setRegistry(parserContext.getRegistry());
        checkElementNameUnique(element);
        Class beanClass = getClassInternal(element);
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

    protected void setRegistry(BeanDefinitionRegistry registry)
    {
        this.registry = registry;
    }

    protected BeanDefinitionRegistry getRegistry()
    {
        if (null == registry)
        {
            throw new IllegalStateException("Set the registry from within doParse");
        }
        return registry;
    }

    protected void checkElementNameUnique(Element element)
    {
        if (null != element.getAttributeNode(ATTRIBUTE_NAME))
        {
            String name = element.getAttribute(ATTRIBUTE_NAME);
            if (getRegistry().containsBeanDefinition(name))
            {
                throw new IllegalArgumentException("A component named " + name + " already exists.");
            }
        }
    }

    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        return BeanDefinitionBuilder.rootBeanDefinition(beanClass);
    }

    protected Class getClassInternal(Element element)
    {
        Class beanClass = null;
        if (isAllowClassAttribute())
        {
            beanClass = getBeanClassFromAttribute(element);
        }
        if (beanClass == null)
        {
            beanClass = getBeanClass(element);
        }
        if (null != beanClass && null != classConstraint && !classConstraint.isAssignableFrom(beanClass))
        {
            logger.error(beanClass + " not a subclass of " + classConstraint);
            beanClass = null;
        }
        if (null == beanClass)
        {
            throw new IllegalStateException("No class for element " + element.getNodeName());
        }
        return beanClass;
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
        BeanAssembler assembler = getBeanAssembler(element, builder);
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            processProperty(attribute, assembler);
        }
        postProcess(assembler, element);
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

    /**
     * Restricted use - does not include a target.
     * If possible, use {@link org.mule.config.spring.parsers.AbstractHierarchicalDefinitionParser#getBeanAssembler(org.w3c.dom.Element, org.springframework.beans.factory.support.BeanDefinitionBuilder)}
     *
     * @param bean The bean being constructed
     * @return An assembler that automates Mule-specific logic for bean construction
     */
    protected BeanAssembler getBeanAssembler(Element element, BeanDefinitionBuilder bean)
    {
        return beanAssemblerFactory.newBeanAssembler(
                propertyConfiguration, bean, propertyConfiguration, null);
    }

    protected boolean isAllowClassAttribute()
    {
        return allowClassAttribute;
    }

    protected void setAllowClassAttribute(boolean allowClassAttribute)
    {
        this.allowClassAttribute = allowClassAttribute;
    }

    protected Class getClassConstraint()
    {
        return classConstraint;
    }

    protected void setClassConstraint(Class classConstraint)
    {
        this.classConstraint = classConstraint;
    }

    protected ParserContext getParserContext()
    {
        return parserContext;
    }

    protected void setParserContext(ParserContext parserContext)
    {
        this.parserContext = parserContext;
    }

    /**
     * @param element The element to test
     * @return true if the element's parent is <mule> or similar
     */
    protected boolean isTopLevel(Element element)
    {
        return element.getParentNode().getLocalName().equals(ROOT_ELEMENT)
                || element.getParentNode().getLocalName().equals(ROOT_UNSAFE_ELEMENT);
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

    public void registerPreProcessor(PreProcessor preProcessor)
    {
        preProcessors.addFirst(preProcessor);
    }

    public void registerPostProcessor(PostProcessor postProcessor)
    {
        postProcessors.add(postProcessor);
    }

}
