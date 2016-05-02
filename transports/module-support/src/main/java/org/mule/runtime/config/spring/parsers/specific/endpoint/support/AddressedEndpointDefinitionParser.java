/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific.endpoint.support;

import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParser;
import org.mule.runtime.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.AttributePropertiesDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.BlockAttribute;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.runtime.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.endpoint.AbstractEndpointBuilder;
import org.mule.runtime.core.endpoint.URIBuilder;

/**
 * Combine a
 * {@link org.mule.runtime.config.spring.parsers.specific.endpoint.support.ChildAddressDefinitionParser} and
 * either a
 * {@link org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser}
 * or a
 * {@link org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser}
 * in one parser.  This lets us put the address attributes in the endpoint element.
 */
public class AddressedEndpointDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{
    public static final boolean META = ChildAddressDefinitionParser.META;
    public static final boolean PROTOCOL = ChildAddressDefinitionParser.PROTOCOL;
    public static final String PROPERTIES = "properties";
    public static final String[] RESTRICTED_ENDPOINT_ATTRIBUTES =
            new String[]{MuleProperties.EXCHANGE_PATTERN,
                    AbstractEndpointBuilder.PROPERTY_RESPONSE_TIMEOUT, "encoding",
                    "connector", "createConnector", "transformer", "responseTransformer", "disableTransportTransformer", "mimeType"};

    // this is an example of parsing a single element with several parsers.  in this case
    // (because we extend AbstractSingleParentFamilyDefinitionParser) the first parser is expected to
    // create the "parent".  then subsequent parsers will be called as children.

    // because all are generated from one element we need to be careful to block attributes
    // that are irrelevant to a particular parser.

    public AddressedEndpointDefinitionParser(String protocol, MuleDefinitionParser endpointParser)
    {
        this(protocol, PROTOCOL, endpointParser);
    }

    public AddressedEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, MuleDefinitionParser endpointParser)
    {
        this(metaOrProtocol, isMeta, endpointParser, new String[]{}, new String[]{});
    }

    public AddressedEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                             MuleDefinitionParser endpointParser,
                                             String[] requiredAddressAttributes,
                                             String[] requiredProperties)
    {
        this(metaOrProtocol, isMeta, endpointParser,
                RESTRICTED_ENDPOINT_ATTRIBUTES, URIBuilder.ALL_ATTRIBUTES,
                new String[][]{requiredAddressAttributes}, new String[][]{requiredProperties});
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param endpointParser The parser for the endpoint
     * @param endpointAttributes A list of attribute names which will be set as properties on the
     * endpoint builder
     * @param addressAttributes A list of attribute names which will be set as properties on the
     * endpoint URI builder
     * @param requiredAddressAttributes A list of attribute names that are required if "address"
     * isn't present
     * @param requiredProperties A list of property names that are required if "address" isn't present
     */
    public AddressedEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                             MuleDefinitionParser endpointParser,
                                             String[] endpointAttributes,
                                             String[] addressAttributes,
                                             String[][] requiredAddressAttributes,
                                             String[][] requiredProperties)
    {
        // the first delegate, the parent, is an endpoint; we block everything except the endpoint attributes
        enableAttributes(endpointParser, endpointAttributes);
        enableAttribute(endpointParser, AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addDelegate(endpointParser);

        // we handle the address and properties separately, setting the
        // properties directly on the endpoint (rather than as part of the address)
        MuleChildDefinitionParser addressParser =
                new AddressParser(metaOrProtocol, isMeta, addressAttributes, requiredAddressAttributes);

        // this handles the exception thrown if a ref is found in the address parser
        addHandledException(BlockAttribute.BlockAttributeException.class);
        addChildDelegate(addressParser);

        MuleChildDefinitionParser propertiesParser =
                new PropertiesParser(PROPERTIES, endpointAttributes, requiredAddressAttributes, requiredProperties);
        addChildDelegate(propertiesParser);
    }

    private static class AddressParser extends ChildAddressDefinitionParser
    {

        public AddressParser(String metaOrProtocol, boolean isMeta,
                             String[] addressAttributes, String[][] requiredAddressAttributes)
        {
            super(metaOrProtocol, isMeta);

            // this handles the "ref problem" - we don't want this parsers to be used if a "ref"
            // defines the address so add a preprocessor to check for that and indicate that the
            // exception should be handled internally, rather than shown to the user.
            // we do this before the extra processors below so that this is called last,
            // allowing other processors to check for conflicts between ref and other attributes
            registerPreProcessor(new BlockAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));

            // the address parser sees only the endpoint attributes
            enableAttributes(this, addressAttributes);

            // we require either a reference, an address, or the attributes specified
            // (properties can be used in parallel with "address")
            String[][] addressAttributeSets =
            new String[(null != requiredAddressAttributes ? requiredAddressAttributes.length : 0) + 2][];
            addressAttributeSets[0] = new String[]{URIBuilder.ADDRESS};
            addressAttributeSets[1] = new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF};
            if (null != requiredAddressAttributes)
            {
                enableAttributes(this, requiredAddressAttributes);
                System.arraycopy(requiredAddressAttributes, 0, addressAttributeSets, 2, requiredAddressAttributes.length);
            }
            registerPreProcessor(new CheckRequiredAttributes(addressAttributeSets));
            // and they must be exclusive
            registerPreProcessor(new CheckExclusiveAttributes(addressAttributeSets));
        }

    }

    private static class PropertiesParser extends AttributePropertiesDefinitionParser
    {

        public PropertiesParser(String setter,
                String[] endpointAttributes, String[][] requiredAddressAttributes, String[][] requiredProperties)
        {
            super(setter);

            // the properties parser gets to see everything that the other parsers don't - if you
            // don't want something, don't enable it in the schema!
            disableAttributes(this, endpointAttributes);
            disableAttributes(this, URIBuilder.ALL_ATTRIBUTES);
            disableAttributes(this, requiredAddressAttributes);
            disableAttribute(this, AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
            disableAttribute(this, AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
            if (null != requiredProperties && requiredProperties.length > 0 &&
                    null != requiredProperties[0] && requiredProperties[0].length > 0)
            {
                // if "ref" is present then we don't complain if required properties are missing, since they
                // must have been provided on the global endpoint
                String[][] requiredPropertiesSets = new String[requiredProperties.length + 1][];
                requiredPropertiesSets[0] = new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF};
                System.arraycopy(requiredProperties, 0, requiredPropertiesSets, 1, requiredProperties.length);
                registerPreProcessor(new CheckRequiredAttributes(requiredPropertiesSets));
            }
        }

    }
}
