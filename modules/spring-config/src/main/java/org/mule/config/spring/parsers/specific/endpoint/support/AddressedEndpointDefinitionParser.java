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
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.generic.AttributePropertiesDefinitionParser;
import org.mule.config.spring.parsers.processors.BlockAttribute;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.impl.endpoint.URIBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;

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
    public static final boolean META = ChildAddressDefinitionParser.META;
    public static final boolean PROTOCOL = ChildAddressDefinitionParser.PROTOCOL;
    public static final boolean URI_PROPERTIES = true;
    public static final boolean SEPARATE_PROPERTIES = false;
    public static final String QUERY_MAP = "queryMap";
    public static final String PROPERTIES = "properties";
    public static final String[] RESTRICTED_ENDPOINT_ATTRIBUTES =
            new String[]{"synchronous", "remoteSync", "remoteSyncTimeout", "encoding",
                    "connector", "createConnector", "transformer", "responseTransformer"};

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
        this(metaOrProtocol, isMeta, URI_PROPERTIES, endpointParser, new String[]{}, new String[]{});
    }

    public AddressedEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, boolean uriProperties,
                                             MuleDefinitionParser endpointParser,
                                             String[] requiredAddressAttributes,
                                             String[] requiredProperties)
    {
        this(metaOrProtocol, isMeta, uriProperties, endpointParser,
                RESTRICTED_ENDPOINT_ATTRIBUTES,
                new String[][]{requiredAddressAttributes}, new String[][]{requiredProperties});
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param uriProperties Whether to add properties to URI or not
     * @param endpointParser The parser for the endpoint
     * @param endpointAttributes A list of attribute names which will be set as properties on the
     * endpoint builder
     * @param requiredAddressAttributes A list of attribute names that are required if "address"
     * isn't present
     * @param requiredProperties A list of property names that are required if "address" isn't present
     */
    public AddressedEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, boolean uriProperties,
                                             MuleDefinitionParser endpointParser,
                                             String[] endpointAttributes,
                                             String[][] requiredAddressAttributes,
                                             String[][] requiredProperties)
    {
        // the first delegate, the parent, is an endpoint; we block everything except the endpoint attributes
        enableAttributes(endpointParser, endpointAttributes);
        enableAttribute(endpointParser, AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addDelegate(endpointParser);

        if (uriProperties)
        {
            // the next delegate parses the address and properties.
            // it will see the endpoint as parent automatically.
            MuleChildDefinitionParser addressParser =
                    new AddressWithPropertiesParser(metaOrProtocol, isMeta,
                            endpointAttributes, requiredAddressAttributes, requiredProperties);

            // this handles the exception thrown if a ref is found in the address parser
            addHandledException(BlockAttribute.BlockAttributeException.class);
            addChildDelegate(addressParser);
        }
        else
        {
            // alternatively, we can handle the address and properties separately, setting the
            // properties directly on the endpoint (rather than as part of the address)
            MuleChildDefinitionParser addressParser =
                    new AddressParser(metaOrProtocol, isMeta, requiredAddressAttributes);

            // this handles the exception thrown if a ref is found in the address parser
            addHandledException(BlockAttribute.BlockAttributeException.class);
            addChildDelegate(addressParser);

            MuleChildDefinitionParser propertiesParser =
                    new PropertiesParser(PROPERTIES, endpointAttributes, requiredAddressAttributes, requiredProperties);
            addChildDelegate(propertiesParser);
        }
    }

    /**
     * This class extends the "chain" of child parsers, adding an address parser with a child
     * properties parser.  The final result is three parsers - the endpoint factory parser,
     * whose child is the address parser, whose child is the properties parser.
     */
    private static class AddressWithPropertiesParser
            extends AbstractSingleParentFamilyDefinitionParser implements MuleChildDefinitionParser
    {

        private MuleChildDefinitionParser addressParser;

        public AddressWithPropertiesParser(String metaOrProtocol, boolean isMeta,
                String[] endpointAttributes, String[][] requiredAddressAttributes, String[][] requiredProperties)
        {
            super(false); // don't reset name!

            // this parses the address.  it will see the endpoint as parent automatically.
            addressParser = new AddressParser(metaOrProtocol, isMeta, requiredAddressAttributes);
            addDelegate(addressParser);

            // the next delegate parses property attributes
            MuleChildDefinitionParser propertiesParser = new PropertiesParser(QUERY_MAP,
                    endpointAttributes, requiredAddressAttributes, requiredProperties);
            propertiesParser.registerPreProcessor(new BlockAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));
            addChildDelegate(propertiesParser);
        }

        public void forceParent(BeanDefinition parent)
        {
            addressParser.forceParent(parent);
        }

        public PropertyConfiguration getTargetPropertyConfiguration()
        {
            return addressParser.getTargetPropertyConfiguration();
        }

    }

    private static class AddressParser extends ChildAddressDefinitionParser
    {

        public AddressParser(String metaOrProtocol, boolean isMeta, String[][] requiredAddressAttributes)
        {
            super(metaOrProtocol, isMeta);

            // this handles the "ref problem" - we don't want this parsers to be used if a "ref"
            // defines the address so add a preprocessor to check for that and indicate that the
            // exception should be handled internally, rather than shown to the user.
            // we do this before the extra processors below so that this is called last,
            // allowing other processors to check for conflicts between ref and other attributes
            registerPreProcessor(new BlockAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));

            // the address parser sees only the endpoint attributes
            enableAttributes(this, URIBuilder.ALL_ATTRIBUTES);
            if (null != requiredAddressAttributes)
            {
                enableAttributes(this, requiredAddressAttributes);
                // we require either a reference, an address, or the attributes specified
                // (properties can be used in parallel with "address")
                String[][] addressAttributeSets = new String[requiredAddressAttributes.length + 2][];
                addressAttributeSets[0] = new String[]{URIBuilder.ADDRESS};
                addressAttributeSets[1] = new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF};
                System.arraycopy(requiredAddressAttributes, 0, addressAttributeSets, 2, requiredAddressAttributes.length);
                registerPreProcessor(new CheckRequiredAttributes(addressAttributeSets));
                // and they must be exclusive
                registerPreProcessor(new CheckExclusiveAttributes(addressAttributeSets));
            }
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
            registerPreProcessor(new CheckRequiredAttributes(requiredProperties));
        }

    }

}
