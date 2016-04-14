/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.UNBOUNDED;
import static org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils.getFirstImplicit;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDynamicParameters;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

/**
 * Builder delegation class to generate a XSD schema that describes a
 * {@link ConfigurationModel}
 *
 * @since 4.0.0
 */
final class ConfigurationSchemaDelegate
{

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final SchemaBuilder builder;
    private Schema schema;

    public ConfigurationSchemaDelegate(SchemaBuilder builder)
    {
        this.builder = builder;
    }

    public void registerConfigElement(Schema schema, final RuntimeConfigurationModel configurationModel)
    {
        this.schema = schema;

        ExtensionType config = registerExtension(configurationModel.getName());
        config.getAttributeOrAttributeGroup().add(builder.createNameAttribute(true));

        final ExplicitGroup choice = new ExplicitGroup();
        choice.setMinOccurs(ZERO);
        choice.setMaxOccurs(UNBOUNDED);

        addConnectionProviderElement(choice, configurationModel);
        addDynamicConfigPolicyElement(choice, configurationModel);
        builder.registerParameters(config, choice, configurationModel.getParameterModels());
        config.setAnnotation(builder.createDocAnnotation(configurationModel.getDescription()));

        configurationModel.getOperationModels().forEach(builder::registerOperation);
        configurationModel.getConnectionProviders().forEach(builder::registerConnectionProviderElement);
        configurationModel.getSourceModels().forEach(builder::registerMessageSource);
    }

    private ExtensionType registerExtension(String name)
    {
        LocalComplexType complexType = new LocalComplexType();

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);
        extension.setComplexType(complexType);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);


        return complexContentExtension;
    }

    private void addConnectionProviderElement(ExplicitGroup all, RuntimeConfigurationModel configurationModel)
    {
        ExtensionModel extensionModel = configurationModel.getExtensionModel();
        if (!extensionModel.getConnectionProviders().isEmpty())
        {
            TopLevelElement objectElement = new TopLevelElement();

            objectElement.setMinOccurs(getFirstImplicit(extensionModel.getConnectionProviders()) != null
                                       ? ZERO
                                       : ONE);

            objectElement.setMaxOccurs("1");
            objectElement.setRef(MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT);

            all.getParticle().add(objectFactory.createElement(objectElement));
        }
    }

    private void addDynamicConfigPolicyElement(ExplicitGroup all, ConfigurationModel configurationModel)
    {
        if (!getDynamicParameters(configurationModel).isEmpty())
        {
            TopLevelElement objectElement = new TopLevelElement();
            objectElement.setMinOccurs(ZERO);
            objectElement.setMaxOccurs("1");
            objectElement.setRef(MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT);

            all.getParticle().add(objectFactory.createElement(objectElement));
        }
    }
}
