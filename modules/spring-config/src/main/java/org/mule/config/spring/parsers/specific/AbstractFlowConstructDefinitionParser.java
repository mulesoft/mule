/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractFlowConstructDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private static final String MODEL_ELEMENT = "model";
    private static final String ABSTRACT_ATTRIBUTE = "abstract";
    private static final String PARENT_ATTRIBUTE = "parent";

    protected static final String ENDPOINT_REF_ATTRIBUTE = "endpoint-ref";
    protected static final String ADDRESS_ATTRIBUTE = "address";

    protected static final String INBOUND_ADDRESS_ATTRIBUTE = "inboundAddress";
    protected static final String INBOUND_ENDPOINT_REF_ATTRIBUTE = "inboundEndpoint-ref";
    protected static final String INBOUND_ENDPOINT_CHILD = "inbound-endpoint";

    protected static final String OUTBOUND_ADDRESS_ATTRIBUTE = "outboundAddress";
    protected static final String OUTBOUND_ENDPOINT_REF_ATTRIBUTE = "outboundEndpoint-ref";
    protected static final String OUTBOUND_ENDPOINT_CHILD = "outboundEndpoint";

    protected static final String TRANSFORMER_REFS_ATTRIBUTE = "transformer-refs";
    protected static final String RESPONSE_TRANSFORMER_REFS_ATTRIBUTE = "responseTransformer-refs";

    @Override
    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class<?> beanClass)
    {
        return BeanDefinitionBuilder.genericBeanDefinition(beanClass);
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if (MODEL_ELEMENT.equals(element.getParentNode().getLocalName()))
        {
            logger.warn("Support for pattern elements in model will be removed from Mule 3.1: move the "
                        + element.getLocalName() + " named '" + element.getAttribute("name")
                        + "' out of model as soon as possible!");
        }

        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        handleAbstractAttribute(element, builder);
        handleParentAttribute(element, builder);
        super.doParse(element, parserContext, builder);
    }

    private void handleParentAttribute(Element element, BeanDefinitionBuilder builder)
    {
        final String parentAttribute = element.getAttribute(PARENT_ATTRIBUTE);
        if (StringUtils.isNotBlank(parentAttribute))
        {
            builder.setParentName(parentAttribute);
        }
        element.removeAttribute(PARENT_ATTRIBUTE);
    }

    private void handleAbstractAttribute(Element element, BeanDefinitionBuilder builder)
    {
        final String abstractAttribute = element.getAttribute(ABSTRACT_ATTRIBUTE);
        if (StringUtils.isNotBlank(abstractAttribute))
        {
            builder.setAbstract(Boolean.parseBoolean(abstractAttribute));
        }
        element.removeAttribute(ABSTRACT_ATTRIBUTE);
    }
}
