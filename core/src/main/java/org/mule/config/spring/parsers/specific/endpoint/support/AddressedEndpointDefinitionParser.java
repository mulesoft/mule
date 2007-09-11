/*
 * $Id:AddressedEndpointDefinitionParser.java 8321 2007-09-10 19:22:52Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.preprocessors.DisableByAttribute;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.util.XmlUtils;

import org.w3c.dom.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Combine a
 * {@link ChildAddressDefinitionParser} and
 * either a
 * {@link OrphanEndpointDefinitionParser}
 * or a
 * {@link ChildEndpointDefinitionParser}
 * in one parser.  This lets us put the address attributes in the endpoint element.
 */
public class AddressedEndpointDefinitionParser extends AbstractSerialDelegatingDefinitionParser
{

    protected Log logger = LogFactory.getLog(getClass());
    public static final String[] BAD_ADDRESS_ATTRIBUTES =
            new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF};
    private String addressId = null;

    public AddressedEndpointDefinitionParser(String protocol, MuleDefinitionParser endpointParser)
    {
        setAddressDelegate(new OrphanAddressDefinitionParser(protocol));
        setEndpointDelegate(endpointParser);
    }

    /**
     * This is called first.  It creates an address using the address-related attributes
     * on the element.
     */
    protected void setAddressDelegate(MuleDefinitionParser delegate)
    {
        enableAttributes(delegate, LazyEndpointURI.ATTRIBUTES, true);

        delegate.registerPreProcessor(new PreProcessor()
        {
            public void preProcess(PropertyConfiguration config, Element element)
            {
                AutoIdUtils.forceUniqueId(element, "address");
                addressId = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
                logger.debug("Generated address name: " + addressId);
            }
        });

        delegate.registerPreProcessor(new DisableByAttribute(BAD_ADDRESS_ATTRIBUTES));

        addDelegate(delegate);
    }

    /**
     * This is called second.  It creates the endpoint and injects the previously created
     * address.
     */
    protected void setEndpointDelegate(MuleDefinitionParser delegate)
    {
        enableAttributes(delegate, LazyEndpointURI.ATTRIBUTES, false);

        delegate.registerPreProcessor(new PreProcessor()
        {
            public void preProcess(PropertyConfiguration config, Element element)
            {
                AutoIdUtils.ensureUniqueId(element, "endpoint");
            }
        });

        delegate.registerPostProcessor(new PostProcessor()
        {
            public void postProcess(BeanAssembler assembler, Element element)
            {
                if (null != addressId)
                {
                    logger.debug("Injecting " + addressId + " in " + XmlUtils.elementToString(element));
                    assembler.extendBean(EndpointUtils.ENDPOINT_URI_ATTRIBUTE, addressId, true);
                    // reset state
                    addressId = null;
                }
            }
        });

        addDelegate(delegate);
    }

}
