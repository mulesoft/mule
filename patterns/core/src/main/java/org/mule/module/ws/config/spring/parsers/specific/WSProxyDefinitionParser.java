/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.config.spring.parsers.specific;

import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.config.spring.parsers.specific.AbstractFlowConstructDefinitionParser;
import org.mule.module.ws.config.spring.factories.WSProxyFactoryBean;
import org.w3c.dom.Element;

public class WSProxyDefinitionParser extends AbstractFlowConstructDefinitionParser
{
    private static final String WSDL_FILE_ATTRIBUTE = "wsdlFile";
    private static final String WSDL_LOCATION_ATTRIBUTE = "wsdlLocation";

    public WSProxyDefinitionParser()
    {
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_REF_ATTRIBUTE}}));

        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{OUTBOUND_ADDRESS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_REF_ATTRIBUTE}}));

        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{WSDL_LOCATION_ATTRIBUTE}, new String[]{WSDL_FILE_ATTRIBUTE}}));

        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            INBOUND_ENDPOINT_REF_ATTRIBUTE, INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_CHILD}));

        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            OUTBOUND_ENDPOINT_REF_ATTRIBUTE, OUTBOUND_ADDRESS_ATTRIBUTE, TRANSFORMER_REFS_ATTRIBUTE,
            RESPONSE_TRANSFORMER_REFS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_CHILD}));
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return WSProxyFactoryBean.class;
    }
}
