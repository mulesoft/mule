/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ZERO;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.DISABLE_VALIDATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MAX_ONE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_POOLING_PROFILE_TYPE;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

/**
 * Builder delegation class to generate a XSD schema that describes a {@link ConnectionProviderModel}
 *
 * @since 4.0.0
 */
final class ConnectionProviderSchemaDelegate {

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  ConnectionProviderSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void registerConnectionProviderElement(ConnectionProviderModel providerModel, DslElementSyntax elementSyntax) {
    Element providerElement = new TopLevelElement();
    providerElement.setName(elementSyntax.getElementName());
    providerElement.setSubstitutionGroup(MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT);

    LocalComplexType complexType = new LocalComplexType();
    providerElement.setComplexType(complexType);

    ExtensionType providerType = new ExtensionType();
    providerType.setBase(MULE_EXTENSION_CONNECTION_PROVIDER_TYPE);

    ComplexContent complexContent = new ComplexContent();
    complexContent.setExtension(providerType);
    complexType.setComplexContent(complexContent);

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(providerElement);


    final ExplicitGroup sequence = new ExplicitGroup();
    sequence.setMinOccurs(ZERO);
    sequence.setMaxOccurs(MAX_ONE);

    builder.addRetryPolicy(sequence);
    ConnectionManagementType managementType = providerModel.getConnectionManagementType();
    if (managementType == POOLING || managementType == CACHED) {
      addValidationFlag(providerType);
    }
    if (managementType == POOLING) {
      addConnectionProviderPoolingProfile(sequence);
    }

    providerModel.getParameterGroupModels().forEach(g -> builder.addParameterGroupsToSequence(
                                                                                              builder
                                                                                                  .registerParameters(providerType,
                                                                                                                      g.getParameterModels()),
                                                                                              sequence));

    providerType.setSequence(sequence);

  }

  private void addConnectionProviderPoolingProfile(ExplicitGroup sequence) {
    TopLevelElement objectElement = builder.createRefElement(MULE_POOLING_PROFILE_TYPE, false);
    sequence.getParticle().add(objectFactory.createElement(objectElement));
  }

  private void addValidationFlag(ExtensionType providerType) {
    providerType.getAttributeOrAttributeGroup()
        .add(builder.createAttribute(DISABLE_VALIDATION, typeLoader.load(boolean.class), false, NOT_SUPPORTED));
  }


}
