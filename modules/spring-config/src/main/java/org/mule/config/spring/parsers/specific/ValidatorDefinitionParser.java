/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.ValidatorFactoryBean;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.w3c.dom.Element;

public class ValidatorDefinitionParser extends AbstractFlowConstructDefinitionParser
{
    public ValidatorDefinitionParser()
    {
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_REF_ATTRIBUTE}}));
        
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{OUTBOUND_ADDRESS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_REF_ATTRIBUTE}}));
        
        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            INBOUND_ENDPOINT_REF_ATTRIBUTE, INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_CHILD}));
        
        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            OUTBOUND_ENDPOINT_REF_ATTRIBUTE, OUTBOUND_ADDRESS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_CHILD}));
    }
    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return ValidatorFactoryBean.class;
    }

}
