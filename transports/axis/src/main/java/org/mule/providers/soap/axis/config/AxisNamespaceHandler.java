/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.processors.AddAttribute;
import org.mule.config.spring.parsers.processors.AttributeConcatenation;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.soap.axis.AxisConnector;

import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;axis:connector&gt;</code> elements.
 */
public class AxisNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(AxisConnector.AXIS);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(AxisConnector.class, true));
        registerBeanDefinitionParser("bean-type", new ChildListEntryDefinitionParser("beanTypes"));
        registerBeanDefinitionParser("supported-scheme", new ChildListEntryDefinitionParser("supportedSchemes"));
        registerMuleBeanDefinitionParser("soap-action", new ChildSingletonMapDefinitionParser("properties")).registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, "soapAction")).addCollection("properties");
        registerBeanDefinitionParser("soap-method", new SoapMethodDefinitionParser());
        registerBeanDefinitionParser("soap-parameter", new SoapParameterDefinitionParser());
    }

    private static final class SoapParameterDefinitionParser extends ParentDefinitionParser
    {

        public SoapParameterDefinitionParser()
        {
            setIgnoredDefault(true);
            removeIgnored("parameter");
            addAlias("parameter", "value");
            addCollection("parameter");
            registerPreProcessor(new AttributeConcatenation("parameter", ";", new String[]{"parameter", "type", "mode"}));
        }

        protected Class getBeanClass(Element element)
        {
            return MapEntryCombiner.class;
        }
    }

    private static final class SoapMethodDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
    {

        public SoapMethodDefinitionParser()
        {
            // children (parameters) want to append to the inner map, not the outer one
            setReturnFirstResult(false);
            addDelegate(new ChildSingletonMapDefinitionParser("properties"))
                    .registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, "soapMethods"))
                    .addCollection("properties")
                    .setIgnoredDefault(true)
                    .removeIgnored(MapEntryCombiner.KEY)
                    .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
            addChildDelegate(new ChildSingletonMapDefinitionParser(MapEntryCombiner.VALUE))
                    .addAlias("method", MapEntryCombiner.KEY)
                    .addCollection(MapEntryCombiner.VALUE)
                    .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        }

    }

}


