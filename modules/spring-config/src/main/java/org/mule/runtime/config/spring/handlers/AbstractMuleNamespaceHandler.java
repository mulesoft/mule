/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.handlers;

import org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.runtime.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.runtime.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.DeprecatedBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParser;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.runtime.config.spring.parsers.PostProcessor;
import org.mule.runtime.config.spring.parsers.PreProcessor;
import org.mule.runtime.config.spring.parsers.assembly.BeanAssembler;
import org.mule.runtime.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.runtime.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * This Namespace handler extends the default Spring {@link org.springframework.beans.factory.xml.NamespaceHandlerSupport}
 * to allow certain elements in document to be ignored by the handler.
 */
public abstract class AbstractMuleNamespaceHandler extends NamespaceHandlerSupport
{
    public static final String GLOBAL_ENDPOINT = "endpoint";
    public static final String INBOUND_ENDPOINT = "inbound-endpoint";
    public static final String OUTBOUND_ENDPOINT = "outbound-endpoint";

    protected transient final Log logger = LogFactory.getLog(getClass());

    protected AbstractMuleNamespaceHandler()
    {
        registerBeanDefinitionParser("annotations", new AnnotationsBeanDefintionParser());
    }

    /**
     * @param name The name of the element to be ignored.
     */
    protected final void registerIgnoredElement(String name)
    {
        registerBeanDefinitionParser(name, new IgnoredDefinitionParser());
    }

    protected MuleDefinitionParserConfiguration registerConnectorDefinitionParser(Class connectorClass, String transportName)
    {
        return registerConnectorDefinitionParser(findConnectorClass(connectorClass, transportName));
    }

    protected MuleDefinitionParserConfiguration registerConnectorDefinitionParser(Class connectorClass)
    {
        return registerConnectorDefinitionParser(new MuleOrphanDefinitionParser(connectorClass, true));
    }

    protected MuleDefinitionParserConfiguration registerConnectorDefinitionParser(MuleDefinitionParser parser)
    {
        registerBeanDefinitionParser("connector", parser);
        return parser;
    }

    protected MuleDefinitionParserConfiguration registerMuleBeanDefinitionParser(String name, MuleDefinitionParser parser)
    {
        registerBeanDefinitionParser(name, parser);
        return parser;
    }

    protected MuleDefinitionParserConfiguration registerStandardTransportEndpoints(String protocol, String[] requiredAttributes)
    {
        return new RegisteredMdps(protocol, AddressedEndpointDefinitionParser.PROTOCOL, requiredAttributes);
    }

    protected MuleDefinitionParserConfiguration registerMetaTransportEndpoints(String protocol)
    {
        return new RegisteredMdps(protocol, AddressedEndpointDefinitionParser.META, new String[] {});
    }

    public static class IgnoredDefinitionParser implements BeanDefinitionParser
    {
        public IgnoredDefinitionParser()
        {
            super();
        }

        @Override
        public BeanDefinition parse(Element element, ParserContext parserContext)
        {
            return null;
        }
    }

    protected Class getInboundEndpointFactoryBeanClass()
    {
        return InboundEndpointFactoryBean.class;
    }

    protected Class getOutboundEndpointFactoryBeanClass()
    {
        return OutboundEndpointFactoryBean.class;
    }

    protected Class getGlobalEndpointBuilderBeanClass()
    {
        return EndpointURIEndpointBuilder.class;
    }

    private class RegisteredMdps implements MuleDefinitionParserConfiguration
    {
        private Set bdps = new HashSet();

        public RegisteredMdps(String protocol, boolean isMeta, String[] requiredAttributes)
        {
            registerBeanDefinitionParser("endpoint",
                    add(new TransportGlobalEndpointDefinitionParser(protocol, isMeta, AbstractMuleNamespaceHandler.this.getGlobalEndpointBuilderBeanClass(), requiredAttributes, new String[] {})));
            registerBeanDefinitionParser("inbound-endpoint",
                    add(new TransportEndpointDefinitionParser(protocol, isMeta, AbstractMuleNamespaceHandler.this.getInboundEndpointFactoryBeanClass(), requiredAttributes, new String[] {})));
            registerBeanDefinitionParser("outbound-endpoint",
                    add(new TransportEndpointDefinitionParser(protocol, isMeta, AbstractMuleNamespaceHandler.this.getOutboundEndpointFactoryBeanClass(), requiredAttributes, new String[] {})));
        }

        private MuleDefinitionParser add(MuleDefinitionParser bdp)
        {
            bdps.add(bdp);
            return bdp;
        }

        @Override
        public MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).registerPreProcessor(preProcessor);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).registerPostProcessor(postProcessor);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addReference(String propertyName)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addReference(propertyName);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addMapping(propertyName, mappings);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addMapping(propertyName, mappings);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addMapping(propertyName, mappings);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addAlias(alias, propertyName);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addCollection(String propertyName)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addCollection(propertyName);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addIgnored(String propertyName)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addIgnored(propertyName);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).removeIgnored(propertyName);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).setIgnoredDefault(ignoreAll);
            }
            return this;
        }

        @Override
        public MuleDefinitionParserConfiguration addBeanFlag(String flag)
        {
            for (Iterator bdp = bdps.iterator(); bdp.hasNext();)
            {
                ((MuleDefinitionParserConfiguration) bdp.next()).addBeanFlag(flag);
            }
            return this;
        }
    }

    static class AnnotationsBeanDefintionParser extends AbstractChildDefinitionParser
    {
        AnnotationsBeanDefintionParser()
        {
            super();
        }

        @Override
        protected AbstractBeanDefinition parseInternal(Element element, ParserContext context)
        {
            AbstractBeanDefinition beanDef = super.parseInternal(element, context);
            beanDef.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, true);
            beanDef.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_REGISTRATION, true);
            return beanDef;
        }

        @Override
        public String getPropertyName(Element element)
        {
            return "annotation";
        }

        @Override
        protected Class<?> getBeanClass(Element element)
        {
            return Map.class;
        }

        @Override
        protected void postProcess(ParserContext context, BeanAssembler beanAssembler, Element element)
        {
            if (beanAssembler instanceof DefaultBeanAssembler)
            {
                DefaultBeanAssembler assembler = (DefaultBeanAssembler) beanAssembler;

                if (assembler.isAnnotationsPropertyAvailable(assembler.getTarget().getBeanClassName()))
                {
                    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
                    {
                        if (node.getNodeType() == Node.ELEMENT_NODE)
                        {
                            StringBuilder builder = new StringBuilder();
                            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
                            {
                                switch (child.getNodeType())
                                {
                                    case Node.TEXT_NODE:
                                    case Node.CDATA_SECTION_NODE:
                                        builder.append(child.getNodeValue());
                                }
                            }
                            assembler.addAnnotationValue(context.getContainingBeanDefinition().getPropertyValues(),
                                                         new QName(node.getNamespaceURI(), node.getLocalName()),
                                                         builder.toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * See if there's a preferred connector class
     */
    protected Class findConnectorClass(Class basicConnector, String transportName)
    {
        String preferredPropertiesURL = "META-INF/services/org/mule/transport/preferred-" +transportName + ".properties";
        InputStream stream = AbstractMuleNamespaceHandler.class.getClassLoader().getResourceAsStream(preferredPropertiesURL);
        if (stream != null)
        {
            try
            {
                Properties preferredProperties = new Properties();
                preferredProperties.load(stream);
                String preferredConnectorName = preferredProperties.getProperty("connector");
                if (preferredConnectorName != null)
                {
                    logger.debug("Found preferred connector class " + preferredConnectorName);
                    return Class.forName(preferredConnectorName);
                }
            }
            catch (Exception e)
            {
                logger.debug("Error processing preferred properties", e);
            }
            finally
            {
                IOUtils.closeQuietly(stream);
            }
        }
        return basicConnector;
    }

    protected void registerDeprecatedBeanDefinitionParser(String elementName, BeanDefinitionParser parser, String message)
    {
        registerBeanDefinitionParser(elementName, new DeprecatedBeanDefinitionParser(
                parser,
                String.format("Schema warning: Use of element <%s> is deprecated.  %s.", elementName, message)));
    }

    protected MuleDefinitionParserConfiguration registerDeprecatedMuleBeanDefinitionParser(String name, MuleDefinitionParser parser, String message)
    {
        registerDeprecatedBeanDefinitionParser(name, parser, message);
        return parser;
    }

    protected MuleDefinitionParserConfiguration registerDeprecatedConnectorDefinitionParser(Class connectorClass, String message)
    {
        MuleOrphanDefinitionParser parser = new MuleOrphanDefinitionParser(connectorClass, true);
        registerDeprecatedBeanDefinitionParser("connector", parser, message);
        return parser;
    }

}