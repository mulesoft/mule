/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import static org.mule.runtime.config.spring.parsers.XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY;
import static org.mule.runtime.core.api.execution.LocationExecutionContextProvider.addMetadataAnnotationsFromXml;
import org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.parsers.assembly.BeanAssembler;
import org.mule.runtime.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.runtime.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.runtime.config.spring.parsers.assembly.configuration.ReusablePropertyConfiguration;
import org.mule.runtime.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.runtime.config.spring.parsers.generic.AutoIdUtils;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.XMLUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * This parser extends the Spring provided {@link AbstractBeanDefinitionParser} to provide additional features for
 * consistently customizing bean representations for Mule bean definition parsers.  Most custom bean definition parsers
 * in Mule will use this base class. The following enhancements are made -
 *
 * <ol>
 * <li>A property name which ends with the suffix "-ref" is assumed to be a reference to another bean.
 * Alternatively, a property can be explicitly registered as a bean reference via registerBeanReference()
 *
 * <p>For example,
 * <code> &lt;bpm:connector bpms-ref=&quot;testBpms&quot;/&gt;</code>
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
    public static final String DOMAIN_ROOT_ELEMENT = "mule-domain";
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_CLASS = "class";
    public static final String ATTRIBUTE_REF = "ref";
    public static final String ATTRIBUTE_REFS = "refs";
    public static final String ATTRIBUTE_REF_SUFFIX = "-" + ATTRIBUTE_REF;
    public static final String ATTRIBUTE_REFS_SUFFIX = "-" + ATTRIBUTE_REFS;

    /**
     * logger used by this class
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private BeanAssemblerFactory beanAssemblerFactory = new DefaultBeanAssemblerFactory();
    protected ReusablePropertyConfiguration beanPropertyConfiguration = new ReusablePropertyConfiguration();
    private ParserContext parserContext;
    private BeanDefinitionRegistry registry;
    private LinkedList<PreProcessor> preProcessors = new LinkedList<PreProcessor>();
    private List<PostProcessor> postProcessors = new LinkedList<PostProcessor>();
    private Set<String> beanAttributes = new HashSet<String>();
    // By default Mule objects are not singletons
    protected boolean singleton = false;

    /** Allow the bean class to be set explicitly via the "class" attribute. */
    private boolean allowClassAttribute = true;
    private Class<?> classConstraint = null;

    public AbstractMuleBeanDefinitionParser()
    {
        addIgnored(ATTRIBUTE_ID);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_FORCE_RECURSE);
    }

    @Override
    public MuleDefinitionParserConfiguration addReference(String propertyName)
    {
        beanPropertyConfiguration.addReference(propertyName);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
    {
        beanPropertyConfiguration.addMapping(propertyName, mappings);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
    {
        beanPropertyConfiguration.addMapping(propertyName, mappings);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
    {
        beanPropertyConfiguration.addMapping(propertyName, mappings);
        return this;
    }

    /**
     * @param alias The attribute name
     * @param propertyName The bean property name
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    @Override
    public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
    {
        beanPropertyConfiguration.addAlias(alias, propertyName);
        return this;
    }

    /**
     * @param propertyName Property that is a collection
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    @Override
    public MuleDefinitionParserConfiguration addCollection(String propertyName)
    {
        beanPropertyConfiguration.addCollection(propertyName);
        return this;
    }

    /**
     * @param propertyName Property that is to be ignored
     * @return This instance, allowing chaining during use, avoiding subclasses
     */
    @Override
    public MuleDefinitionParserConfiguration addIgnored(String propertyName)
    {
        beanPropertyConfiguration.addIgnored(propertyName);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
    {
        beanPropertyConfiguration.removeIgnored(propertyName);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
    {
        beanPropertyConfiguration.setIgnoredDefault(ignoreAll);
        return this;
    }

    protected void processProperty(Attr attribute, BeanAssembler assembler)
    {
        assembler.extendBean(attribute);
    }

    /**
     * Hook method that derived classes can implement to inspect/change a
     * bean definition after parsing is complete.
     *
     * @param assembler the parsed (and probably totally defined) bean definition being built
     * @param element   the XML element that was the source of the bean definition's metadata
     */
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        element.setAttribute(ATTRIBUTE_NAME, getBeanName(element));
        for (String attribute : beanAttributes)
        {
            assembler.setBeanFlag(attribute);
        }
        for (PostProcessor processor : postProcessors)
        {
            processor.postProcess(context, assembler, element);
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
        beanPropertyConfiguration.reset();
        for (PreProcessor processor : preProcessors)
        {
            processor.preProcess(beanPropertyConfiguration, element);
        }
    }

    /**
     * Creates a {@link BeanDefinitionBuilder} instance for the {@link #getBeanClass
     * bean Class} and passes it to the {@link #doParse} strategy method.
     *
     * @param element the element that is to be parsed into a single BeanDefinition
     * @param context the object encapsulating the current state of the parsing
     *            process
     * @return the BeanDefinition resulting from the parsing of the supplied
     *         {@link Element}
     * @throws IllegalStateException if the bean {@link Class} returned from
     *             {@link #getBeanClass(org.w3c.dom.Element)} is <code>null</code>
     * @see #doParse
     */
    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext context)
    {
        preProcess(element);
        setParserContext(context);
        setRegistry(context.getRegistry());
        checkElementNameUnique(element);
        Class<?> beanClass = getClassInternal(element);
        BeanDefinitionBuilder builder = createBeanDefinitionBuilder(element, beanClass);
        builder.getRawBeanDefinition().setSource(context.extractSource(element));
        builder.setScope(isSingleton() ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);

        if (FactoryBean.class.isAssignableFrom(beanClass))
        {
            if (Initialisable.class.isAssignableFrom(beanClass))
            {
                builder.setInitMethodName(Initialisable.PHASE_NAME);
            }

            if (Disposable.class.isAssignableFrom(beanClass))
            {
                builder.setDestroyMethodName(Disposable.PHASE_NAME);
            }
        }

        if (context.isNested())
        {
            // Inner bean definition must receive same singleton status as containing bean.
            builder.setScope(context.getContainingBeanDefinition().isSingleton()
                             ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
        }

        doParse(element, context, builder);
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
        BeanDefinitionFactory.checkElementNameUnique(getRegistry(), element);
    }

    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class<?> beanClass)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanClass);
        // If a constructor with a single MuleContext argument is available then use it.
        if (ClassUtils.getConstructor(beanClass, new Class[]{MuleContext.class}, true) != null)
        {
            builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
        }
        return builder;
    }

    protected Class<?> getClassInternal(Element element)
    {
        Class<?> beanClass = null;
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
            throw new IllegalStateException(beanClass + " not a subclass of " + classConstraint +
                    " for " + XMLUtils.elementToString(element));
        }
        if (null == beanClass)
        {
            throw new IllegalStateException("No class for element " + XMLUtils.elementToString(element));
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
    protected Class<?> getBeanClassFromAttribute(Element element)
    {
        String att = beanPropertyConfiguration.getAttributeAlias(ATTRIBUTE_CLASS);
        String className = element.getAttribute(att);
        Class<?> clazz = null;
        if (StringUtils.isNotBlank(className))
        {
            try
            {
                element.removeAttribute(att);
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
    protected abstract Class<?> getBeanClass(Element element);

    /**
     * Parse the supplied {@link Element} and populate the supplied
     * {@link BeanDefinitionBuilder} as required.
     * <p>
     * The default implementation delegates to the <code>doParse</code> version
     * without ParserContext argument.
     *
     * @param element the XML element being parsed
     * @param context the object encapsulating the current state of the parsing
     *            process
     * @param builder used to define the <code>BeanDefinition</code>
     */
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        BeanAssembler assembler = getBeanAssembler(element, builder);

        processMetadataAnnotations(element, getConfigFileIdentifier(context.getReaderContext().getResource()), builder);

        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            processProperty(attribute, assembler);
        }

        postProcess(getParserContext(), assembler, element);
    }

    protected void processMetadataAnnotations(Element element, String configFileIdentifier, BeanDefinitionBuilder builder)
    {
        processMetadataAnnotationsHelper(element, configFileIdentifier, builder);
    }

    public static String getConfigFileIdentifier(Resource resource)
    {
        return resource.getFilename() != null ? resource.getFilename() : resource.getDescription();
    }

    public static Map<QName, Object> processMetadataAnnotationsHelper(Element element, String configFileIdentifier, BeanDefinitionBuilder builder)
    {
        Map<QName, Object> annotations = new HashMap<>();
        //TODO MULE-9638 - Remove once we don't use the old parsing mechanism anymore.
        if (element == null)
        {
            return annotations;
        }
        // Ensure we have a placeholder for internally generated annotations, even if the XML config doesn't have any
        // defined for this element.
        if (AnnotatedObject.class.isAssignableFrom(builder.getBeanDefinition().getBeanClass()))
        {

            XmlMetadataAnnotations elementMetadata = (XmlMetadataAnnotations) element.getUserData(METADATA_ANNOTATIONS_KEY);
            addMetadataAnnotationsFromXml(annotations, configFileIdentifier,
                    elementMetadata.getLineNumber(), elementMetadata.getElementString());

            builder.getBeanDefinition().getPropertyValues().addPropertyValue(AnnotatedObject.PROPERTY_NAME, annotations);
        }
        return annotations;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition,
        ParserContext context) throws BeanDefinitionStoreException
    {
        return getBeanName(element);
    }

    protected boolean isSingleton()
    {
        return singleton;
    }

    /**
     * Restricted use - does not include a target.
     * If possible, use {@link org.mule.runtime.config.spring.parsers.AbstractHierarchicalDefinitionParser#getBeanAssembler(org.w3c.dom.Element, org.springframework.beans.factory.support.BeanDefinitionBuilder)}
     *
     * @param bean The bean being constructed
     * @return An assembler that automates Mule-specific logic for bean construction
     */
    protected BeanAssembler getBeanAssembler(Element element, BeanDefinitionBuilder bean)
    {
        return getBeanAssemblerFactory().newBeanAssembler(
                beanPropertyConfiguration, bean, beanPropertyConfiguration, null);
    }

    protected boolean isAllowClassAttribute()
    {
        return allowClassAttribute;
    }

    protected void setAllowClassAttribute(boolean allowClassAttribute)
    {
        this.allowClassAttribute = allowClassAttribute;
    }

    protected Class<?> getClassConstraint()
    {
        return classConstraint;
    }

    protected void setClassConstraint(Class<?> classConstraint)
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
        return element.getParentNode().getLocalName().equals(ROOT_ELEMENT) || element.getParentNode().getLocalName().equals(DOMAIN_ROOT_ELEMENT);
    }

    @Override
    public AbstractBeanDefinition muleParse(Element element, ParserContext context)
    {
        return parseInternal(element, context);
    }

    @Override
    public MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor)
    {
        preProcessors.addFirst(preProcessor);
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor)
    {
        postProcessors.add(postProcessor);
        return this;
    }

    public BeanAssemblerFactory getBeanAssemblerFactory()
    {
        return beanAssemblerFactory;
    }

    public void setBeanAssemblerFactory(BeanAssemblerFactory beanAssemblerFactory)
    {
        this.beanAssemblerFactory = beanAssemblerFactory;
    }

    @Override
    public String getBeanName(Element element)
    {
        return AutoIdUtils.getUniqueName(element, "mule-bean");
    }

    @Override
    public MuleDefinitionParserConfiguration addBeanFlag(String flag)
    {
        beanAttributes.add(flag);
        return this;
    }
}
