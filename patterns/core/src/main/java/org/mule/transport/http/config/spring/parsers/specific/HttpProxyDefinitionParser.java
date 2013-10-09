/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.config.spring.parsers.specific;

import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.config.spring.parsers.specific.AbstractFlowConstructDefinitionParser;
import org.mule.transport.http.config.spring.factories.HttpProxyFactoryBean;
import org.w3c.dom.Element;

public class HttpProxyDefinitionParser extends AbstractFlowConstructDefinitionParser
{
    public HttpProxyDefinitionParser()
    {
        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_REF_ATTRIBUTE}}));

        super.registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
            new String[]{OUTBOUND_ADDRESS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_REF_ATTRIBUTE}}));

        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            INBOUND_ENDPOINT_REF_ATTRIBUTE, INBOUND_ADDRESS_ATTRIBUTE}, new String[]{INBOUND_ENDPOINT_CHILD}));

        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            OUTBOUND_ENDPOINT_REF_ATTRIBUTE, OUTBOUND_ADDRESS_ATTRIBUTE, TRANSFORMER_REFS_ATTRIBUTE,
            RESPONSE_TRANSFORMER_REFS_ATTRIBUTE}, new String[]{OUTBOUND_ENDPOINT_CHILD}));
    }

    @Override
    protected Class<?> getBeanClass(final Element element)
    {
        return HttpProxyFactoryBean.class;
    }
}
