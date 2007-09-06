/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.delegate.DelegateDefinitionParser;
import org.mule.config.spring.parsers.delegate.PostProcessor;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.StringUtils;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import org.w3c.dom.Element;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public class EndpointUtils
{

    public static final String CONNECTOR_ATTRIBUTE = "connector";
    public static final String TRANSFORMERS_ATTRIBUTE = "transformers";
    public static final String RESPONSE_TRANSFORMERS_ATTRIBUTE = "responseTransformers";
    public static final String ENDPOINT_URI_ATTRIBUTE = "endpointURI";
    public static final String ADDRESS_ATTRIBUTE = "address";
    public static final String ENDPOINT_REF_ATTRIBUTE = "ref";

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
        if (StringUtils.isNotBlank(element.getAttribute(CONNECTOR_ATTRIBUTE)))
        {
            assembler.getBean().addDependsOn(element.getAttribute(CONNECTOR_ATTRIBUTE));
        }
    }

    public static void addPostProcess(DelegateDefinitionParser parser)
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

    public static void addConditions(DelegateDefinitionParser parser)
    {
        parser.addAlias(ADDRESS_ATTRIBUTE, ENDPOINT_URI_ATTRIBUTE);
        parser.addMapping("createConnector", "GET_OR_CREATE=0,ALWAYS_CREATE=1,NEVER_CREATE=2");
        parser.addAlias("transformers", "transformer");
        parser.addAlias("responseTransformers", "responseTransformer");
        parser.addIgnored(ENDPOINT_REF_ATTRIBUTE);
    }

    public static BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        if (null == element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
        {
            return null;
        }
        else
        {
            String parent = element.getAttribute(ENDPOINT_REF_ATTRIBUTE);
            BeanDefinitionBuilder bdb = BeanDefinitionBuilder.childBeanDefinition(parent);
            bdb.getBeanDefinition().setBeanClassName(beanClass.getName());
            // need to overload the type so it becomes a local endpoint
            bdb.addPropertyValue("type", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
            return bdb;
        }
    }

}
