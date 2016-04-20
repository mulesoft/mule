/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ZERO;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.DISABLE_VALIDATION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_POOLING_PROFILE_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.UNBOUNDED;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.PoolingSupport;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.property.ConnectionHandlingTypeModelProperty;
import org.mule.metadata.api.ClassTypeLoader;
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
 * {@link ConnectionProviderModel}
 *
 * @since 4.0.0
 */
final class ConnectionProviderSchemaDelegate
{

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final SchemaBuilder builder;
    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

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

        providerModel.getModelProperty(ConnectionHandlingTypeModelProperty.class).ifPresent(connectionHandlingType -> {
            if (connectionHandlingType.isPooled() || connectionHandlingType.isCached())
            {
                addValidationFlag(providerType);
                builder.addRetryPolicy(choice);
            }
            if (connectionHandlingType.isPooled())
            {
                addConnectionProviderPoolingProfile(choice, providerModel);
            }
        });

        builder.registerParameters(providerType, choice, providerModel.getParameterModels());
    }

    private void addConnectionProviderPoolingProfile(ExplicitGroup choice, ConnectionProviderModel providerModel)
    {
        PoolingSupport poolingSupport = providerModel.getModelProperty(ConnectionHandlingTypeModelProperty.class).get().getPoolingSupport();
        TopLevelElement objectElement = builder.createRefElement(MULE_POOLING_PROFILE_TYPE, poolingSupport == PoolingSupport.REQUIRED);

        choice.getParticle().add(objectFactory.createElement(objectElement));
    }

    private void addValidationFlag(ExtensionType providerType)
    {
        providerType.getAttributeOrAttributeGroup().add(builder.createAttribute(DISABLE_VALIDATION, typeLoader.load(boolean.class), false, NOT_SUPPORTED));
    }


}
