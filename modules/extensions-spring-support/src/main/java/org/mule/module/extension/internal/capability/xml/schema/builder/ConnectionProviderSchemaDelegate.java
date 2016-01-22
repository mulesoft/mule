/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.DISABLE_VALIDATION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_POOLING_PROFILE_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.UNBOUNDED;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.PoolingSupport;
import org.mule.extension.api.introspection.property.ConnectionHandlingTypeModelProperty;
import org.mule.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.module.extension.internal.capability.xml.schema.model.TopLevelElement;

/**
 * Builder delegation class to generate a XSD schema that describes a
 * {@link ConnectionProviderModel}
 *
 * @since 4.0.0
 */
final class ConnectionProviderSchemaDelegate
{

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final SchemaBuilder builder;

    public ConnectionProviderSchemaDelegate(SchemaBuilder builder)
    {
        this.builder = builder;
    }

    public void registerConnectionProviderElement(Schema schema, ConnectionProviderModel providerModel)
    {
        Element providerElement = new TopLevelElement();
        providerElement.setName(providerModel.getName());
        providerElement.setSubstitutionGroup(MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT);

        LocalComplexType complexType = new LocalComplexType();
        providerElement.setComplexType(complexType);

        ExtensionType providerType = new ExtensionType();
        providerType.setBase(MULE_EXTENSION_CONNECTION_PROVIDER_TYPE);

        ComplexContent complexContent = new ComplexContent();
        complexContent.setExtension(providerType);
        complexType.setComplexContent(complexContent);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(providerElement);

        final ExplicitGroup choice = new ExplicitGroup();
        choice.setMinOccurs(ZERO);
        choice.setMaxOccurs(UNBOUNDED);

        ConnectionHandlingTypeModelProperty connectionHandlingType = providerModel.getModelProperty(ConnectionHandlingTypeModelProperty.KEY);

        if (connectionHandlingType != null)
        {
            if (connectionHandlingType.isPooled() || connectionHandlingType.isCached())
            {
                addValidationFlag(providerType);
                addConnectionProviderRetryPolicy(choice);
            }
            if (connectionHandlingType.isPooled())
            {
                addConnectionProviderPoolingProfile(choice, providerModel);
            }
        }

        builder.registerParameters(providerType, choice, providerModel.getParameterModels());
    }

    private void addConnectionProviderPoolingProfile(ExplicitGroup choice, ConnectionProviderModel providerModel)
    {
        ConnectionHandlingTypeModelProperty connectionHandlingType = providerModel.getModelProperty(ConnectionHandlingTypeModelProperty.KEY);
        TopLevelElement objectElement = new TopLevelElement();

        objectElement.setMinOccurs(connectionHandlingType.getPoolingSupport() == PoolingSupport.REQUIRED ? ONE : ZERO);
        objectElement.setMaxOccurs("1");
        objectElement.setRef(MULE_POOLING_PROFILE_TYPE);

        choice.getParticle().add(objectFactory.createElement(objectElement));
    }

    private void addValidationFlag(ExtensionType providerType)
    {
        providerType.getAttributeOrAttributeGroup().add(builder.createAttribute(DISABLE_VALIDATION, DataType.of(boolean.class), false, NOT_SUPPORTED));
    }

    private void addConnectionProviderRetryPolicy(ExplicitGroup choice)
    {
        TopLevelElement providerElementRetry = new TopLevelElement();
        providerElementRetry.setMinOccurs(ZERO);
        providerElementRetry.setMaxOccurs("1");
        providerElementRetry.setRef(MULE_ABSTRACT_RECONNECTION_STRATEGY);

        choice.getParticle().add(objectFactory.createElement(providerElementRetry));
    }

}
