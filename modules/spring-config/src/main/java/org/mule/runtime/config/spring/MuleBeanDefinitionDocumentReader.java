/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Collections.emptyList;
import static org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser.getConfigFileIdentifier;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.core.api.MuleRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the
 * parsing of custom spring bean elements nested within each other
 */
public class MuleBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader
{

    private final BeanDefinitionFactory beanDefinitionFactory;
    private final XmlApplicationParser xmlApplicationParser;
    //This same instance is called several time to parse different XML files so a stack is needed to save previous state.
    private final Stack<ApplicationModel> applicationModelStack = new Stack<>();

    public MuleBeanDefinitionDocumentReader(BeanDefinitionFactory beanDefinitionFactory, XmlApplicationParser xmlApplicationParser)
    {
        this.beanDefinitionFactory = beanDefinitionFactory;
        this.xmlApplicationParser = xmlApplicationParser;
    }

    @Override
    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate)
    {
        BeanDefinitionParserDelegate delegate = createBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    protected MuleHierarchicalBeanDefinitionParserDelegate createBeanDefinitionParserDelegate(XmlReaderContext readerContext)
    {
        return new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this, applicationModelStack::peek, beanDefinitionFactory, getElementsValidator());
    }

    protected ElementValidator[] getElementsValidator()
    {
        return new ElementValidator[0];
    }

    /**
     * Override to reject configuration files with no namespace, e.g. mule legacy
     * configuration file.
     */
    @Override
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate)
    {
        if (!StringUtils.hasLength(root.getNamespaceURI()))
        {
            getReaderContext().error("Unable to locate NamespaceHandler for namespace [null]", root);
        }
        else
        {
            super.parseBeanDefinitions(root, delegate);
        }
    }

    @Override
    protected void preProcessXml(Element root)
    {
        try
        {
            List<ConfigLine> configLines = new ArrayList<>();
            configLines.add(xmlApplicationParser.parse(root).get());
            ArtifactConfig artifactConfig = new ArtifactConfig.Builder().addConfigFile(new ConfigFile(getConfigFileIdentifier(getReaderContext().getResource()), configLines)).build();
            applicationModelStack.push(new ApplicationModel(artifactConfig, new ArtifactConfiguration(emptyList())));
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    protected void postProcessXml(Element root)
    {
        applicationModelStack.pop();
    }

}
