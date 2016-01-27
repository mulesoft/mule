/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.DISABLE_VALIDATION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_POOLING_PROFILE_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.POOLING_PROFILE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.RETRY_POLICY_TEMPLATE;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.parseConnectionProviderName;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;

import org.mule.api.config.PoolingProfile;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Implementation of {@link BaseExtensionBeanDefinitionParser} capable of parsing instances
 * which are compliant with the model defined in a {@link #providerModel}. The outcome of
 * this parser will be a {@link ConnectionProviderModel}.
 * <p>
 * It supports simple attributes, pojos, lists/sets of simple attributes, list/sets of beans,
 * and maps of simple attributes.
 *
 * @since 4.0
 */
final class ConnectionProviderBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final ConnectionProviderModel providerModel;

    public ConnectionProviderBeanDefinitionParser(ConnectionProviderModel providerModel)
    {
        super(ConnectionProviderFactoryBean.class);
        this.providerModel = providerModel;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, ParserContext parserContext)
    {
        if (element.getParentNode().getNamespaceURI().equals(MULE_NAMESPACE))
        {
            parseConnectionProviderName(element, builder);
        }

        builder.addConstructorArgValue(providerModel);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));

        if (StringUtils.isNotEmpty(element.getAttribute(DISABLE_VALIDATION)))
        {
            builder.addPropertyValue(DISABLE_VALIDATION, element.getAttribute(DISABLE_VALIDATION));
        }

        for (Element childElement : DomUtils.getChildElements(element))
        {
            if (childElement.getSchemaTypeInfo().isDerivedFrom(MULE_NAMESPACE, MULE_ABSTRACT_POOLING_PROFILE_TYPE.getLocalPart(), DERIVATION_EXTENSION))
            {
                builder.addPropertyValue(POOLING_PROFILE, getPoolingProfileParser().parse(childElement, parserContext));
            }

            if (childElement.getSchemaTypeInfo().isDerivedFrom(MULE_NAMESPACE, MULE_ABSTRACT_RECONNECTION_STRATEGY_TYPE.getLocalPart(), DERIVATION_EXTENSION))
            {
                BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(childElement);
                builder.addPropertyValue(RETRY_POLICY_TEMPLATE, beanDefinition);
            }
        }
    }

    //TODO: MULE-9047 should have a better way of parsing this
    private BeanDefinitionParser getPoolingProfileParser()
    {
        OrphanDefinitionParser poolingProfileParser = new OrphanDefinitionParser(PoolingProfile.class, true);
        poolingProfileParser.addMapping("initialisationPolicy", PoolingProfile.POOL_INITIALISATION_POLICIES);
        poolingProfileParser.addMapping("exhaustedAction", PoolingProfile.POOL_EXHAUSTED_ACTIONS);
        return poolingProfileParser;
    }
}
