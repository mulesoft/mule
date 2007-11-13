/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

/**
 * Routines and constants common to the two endpoint definition parsers.
 *
 * @see ChildEndpointDefinitionParser
 * @see OrphanEndpointDefinitionParser
 */
public class EndpointUtils
{

    public static final String CONNECTOR_ATTRIBUTE = "connector";
    public static final String TRANSFORMERS_ATTRIBUTE = "transformers";
    public static final String RESPONSE_TRANSFORMERS_ATTRIBUTE = "responseTransformers";
    public static final String ENDPOINT_BUILDER_ATTRIBUTE = "endpointBuilder";
    public static final String ADDRESS_ATTRIBUTE = "address";

    private static void processTransformerDependencies(BeanAssembler assembler, Element element, String attributeName)
    {
        if(StringUtils.isNotBlank(element.getAttribute(attributeName)))
        {
            String[] trans = StringUtils.split(element.getAttribute(attributeName), " ,;");
            for (int i = 0; i < trans.length; i++)
            {
                assembler.getBean().addDependsOn(trans[i]);
            }
        }
    }

    private static void processConnectorDependency(BeanAssembler assembler, Element element)
    {
        // does this ever work - it doesn't match "connector-ref"
        if (StringUtils.isNotBlank(element.getAttribute(CONNECTOR_ATTRIBUTE)))
        {
            assembler.getBean().addDependsOn(element.getAttribute(CONNECTOR_ATTRIBUTE));
        }
    }

    public static void addPostProcess(MuleDefinitionParser parser)
    {
        parser.registerPostProcessor(new PostProcessor()
        {
            public void postProcess(BeanAssembler assembler, Element element)
            {
                EndpointUtils.processConnectorDependency(assembler, element);
                EndpointUtils.processTransformerDependencies(assembler, element, EndpointUtils.TRANSFORMERS_ATTRIBUTE);
                EndpointUtils.processTransformerDependencies(assembler, element, EndpointUtils.RESPONSE_TRANSFORMERS_ATTRIBUTE);
            }
        });
    }

    public static void addProperties(MuleDefinitionParser parser)
    {
        parser.addAlias(ADDRESS_ATTRIBUTE, ENDPOINT_BUILDER_ATTRIBUTE);
        parser.addMapping("createConnector", "GET_OR_CREATE=0,ALWAYS_CREATE=1,NEVER_CREATE=2");
    }

}
