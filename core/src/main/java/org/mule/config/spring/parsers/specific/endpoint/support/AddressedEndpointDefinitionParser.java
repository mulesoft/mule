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
import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.generic.AttributePropertiesDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.preprocessors.DisableByAttribute;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Combine a
 * {@link org.mule.config.spring.parsers.specific.endpoint.support.ChildAddressDefinitionParser} and
 * either a
 * {@link org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser}
 * or a
 * {@link org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser}
 * in one parser.  This lets us put the address attributes in the endpoint element.
 */
public class AddressedEndpointDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    protected Log logger = LogFactory.getLog(getClass());
    public static final String[] BAD_ADDRESS_ATTRIBUTES =
            new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF};

    // this is an example of parsing a single element with several parsers.  in this case
    // (because we extend SingleParentFamilyDefinitionParser) the first parser is expected to
    // create the "parent".  then subsequent parsers will be called as children.

    // because all are generated from one element we need to be careful to block attributes
    // that are irrelevant to a particular parser.

    public AddressedEndpointDefinitionParser(String protocol, MuleDefinitionParser endpointParser)
    {
        this(protocol, endpointParser, new String[]{});
    }

    /**
     * @param protocol The transport protocol ("tcp" etc)
     * @param endpointParser The parser for the endpoint
     * @param propertyAttributes A list of attribute names which will be set as properties on the
     * endpointParser
     */
    public AddressedEndpointDefinitionParser(String protocol, MuleDefinitionParser endpointParser,
                                             String[] propertyAttributes)
    {
        // the first delegate, the parent, is an endpoint; we block address and property
        // related attributes
        disableAttributes(endpointParser, LazyEndpointURI.ATTRIBUTES);
        disableAttributes(endpointParser, propertyAttributes);
        addDelegate(endpointParser);

        // the next delegate parses the address.  it will see the endpoint as parent automatically.
        MuleChildDefinitionParser addressParser = new ChildAddressDefinitionParser(protocol);
        // it should see only the endpoint attributes
        enableAttributes(addressParser, LazyEndpointURI.ATTRIBUTES);
        addChildDelegate(addressParser);

        // the next delegate parses property attributes
        MuleChildDefinitionParser propertiesParser = new AttributePropertiesDefinitionParser("properties");
        enableAttributes(propertiesParser, propertyAttributes);
        addChildDelegate(propertiesParser);

        // this handles the "ref problem" - we don't want these parsers to be used if a "ref"
        // defines the address so add a preprocessor to check for that and indicate that the
        // exception should be handled internally, rather than shown to the user
        addressParser.registerPreProcessor(new DisableByAttribute(BAD_ADDRESS_ATTRIBUTES));
        propertiesParser.registerPreProcessor(new DisableByAttribute(BAD_ADDRESS_ATTRIBUTES));
        addHandledException(DisableByAttribute.DisableByAttributeException.class);
    }

}