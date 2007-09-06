/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.delegate.AbstractDelegateDelegate;
import org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser;
import org.mule.config.spring.parsers.delegate.PostProcessor;
import org.mule.config.spring.parsers.delegate.PreProcessor;
import org.mule.config.spring.parsers.generic.AutoIdUtils;

import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Combine an {@link ChildAddressDefinitionParser} and
 * an {@link UnaddressedEndpointDefinitionParser} in
 * one parser.  This lets us put the address attributes in the endpoint element.
 */
public class AddressedEndpointDefinitionParser extends AbstractSerialDelegatingDefinitionParser
{

    private String addressId;

    public AddressedEndpointDefinitionParser(String protocol, Class endpoint)
    {
        addDelegate(new AddressDelegate(protocol));
        addDelegate(new EndpointDelegate(endpoint));
    }

    /**
     * This is called first.  It creates an address using the address-related attributes
     * on the element.
     */
    private class AddressDelegate extends AbstractDelegateDelegate
    {

        private AddressDelegate(String protocol)
        {
            super(new OrphanAddressDefinitionParser(protocol));
            getDelegate().setIgnoredDefault(true);
            Iterator names = Arrays.asList(LazyEndpointURI.ATTRIBUTES).iterator();
            while (names.hasNext())
            {
                getDelegate().removeIgnored((String) names.next());
            }
            registerPreProcessor(new PreProcessor()
            {
                public void preProcess(Element element)
                {
                    AutoIdUtils.forceUniqueId(element, "address");
                    addressId = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
                }
            });
        }

    }

    /**
     * This is called second.  It creates the endpoint and injects the previously created
     * address.
     */
    private class EndpointDelegate extends AbstractDelegateDelegate
    {

        private EndpointDelegate(Class endpoint)
        {
            super(new UnaddressedEndpointDefinitionParser(endpoint));
            Iterator names = Arrays.asList(LazyEndpointURI.ATTRIBUTES).iterator();
            while (names.hasNext())
            {
                getDelegate().addIgnored((String) names.next());
            }
            registerPreProcessor(new PreProcessor()
            {
                public void preProcess(Element element)
                {
                    AutoIdUtils.ensureUniqueId(element, "endpoint");
                }
            });
            registerPostProcessor(new PostProcessor()
            {
                public void postProcess(BeanAssembler assembler, Element element)
                {
                    assembler.extendBean(UnaddressedEndpointDefinitionParser.ENDPOINT_URI_ATTRIBUTE, addressId, true);
                }
            });
        }

    }

}
