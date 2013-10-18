/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.util.StringUtils;

import org.w3c.dom.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Routines and constants common to the two endpoint definition parsers.
 *
 * @see ChildEndpointDefinitionParser
 * @see OrphanEndpointDefinitionParser
 */
public class EndpointUtils
{

    private static Log logger = LogFactory.getLog(EndpointUtils.class);
    public static final String CONNECTOR_ATTRIBUTE = "connector-ref";
    public static final String TRANSFORMERS_ATTRIBUTE = "transformer-refs";
    public static final String URI_BUILDER_ATTRIBUTE = "URIBuilder";
    public static final String ADDRESS_ATTRIBUTE = "address";

    private static void processTransformerDependencies(BeanAssembler assembler, Element element)
    {
        if (StringUtils.isNotBlank(element.getAttribute(TRANSFORMERS_ATTRIBUTE)))
        {
            String[] trans = StringUtils.split(element.getAttribute(TRANSFORMERS_ATTRIBUTE), " ,;");
            for (int i = 0; i < trans.length; i++)
            {
                String ref = trans[i];
                if (logger.isDebugEnabled())
                {
                    logger.debug("transformer dep: " + ref);
                }
                assembler.getBean().addDependsOn(ref);
            }
        }
    }

    private static void processConnectorDependency(BeanAssembler assembler, Element element)
    {
        if (StringUtils.isNotBlank(element.getAttribute(CONNECTOR_ATTRIBUTE)))
        {
            String ref = element.getAttribute(CONNECTOR_ATTRIBUTE);
            if (logger.isDebugEnabled())
            {
                logger.debug("connector dep: " + ref);
            }
            assembler.getBean().addDependsOn(ref);
        }
    }

    public static void addPostProcess(MuleDefinitionParser parser)
    {
        parser.registerPostProcessor(new PostProcessor()
        {
            public void postProcess(ParserContext unused, BeanAssembler assembler, Element element)
            {
                EndpointUtils.processConnectorDependency(assembler, element);
                EndpointUtils.processTransformerDependencies(assembler, element);
            }
        });
    }

    public static void addProperties(MuleDefinitionParser parser)
    {
        parser.addAlias(ADDRESS_ATTRIBUTE, URI_BUILDER_ATTRIBUTE);
        parser.addAlias("transformer", "transformers");
        parser.addAlias("responseTransformer", "responseTransformers");
        parser.addMapping("createConnector", "GET_OR_CREATE=0,ALWAYS_CREATE=1,NEVER_CREATE=2");
    }

}
