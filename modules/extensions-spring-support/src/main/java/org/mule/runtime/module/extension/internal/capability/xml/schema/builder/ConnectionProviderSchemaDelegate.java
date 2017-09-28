/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ZERO;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_CONNECTION_PROVIDER_TYPE;

import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

/**
 * Builder delegation class to generate a XSD schema that describes a {@link ConnectionProviderModel}
 *
 * @since 4.0.0
 */
final class ConnectionProviderSchemaDelegate {

  private final SchemaBuilder builder;

  ConnectionProviderSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void registerConnectionProviderElement(ConnectionProviderModel providerModel, DslElementSyntax elementSyntax) {
    Element providerElement = new TopLevelElement();
    providerElement.setName(elementSyntax.getElementName());
    providerElement.setSubstitutionGroup(MULE_CONNECTION_PROVIDER_ELEMENT);

    LocalComplexType complexType = new LocalComplexType();
    providerElement.setComplexType(complexType);

    ExtensionType providerType = new ExtensionType();
    providerType.setBase(MULE_CONNECTION_PROVIDER_TYPE);

    ComplexContent complexContent = new ComplexContent();
    complexContent.setExtension(providerType);
    complexType.setComplexContent(complexContent);

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(providerElement);


    final ExplicitGroup sequence = new ExplicitGroup();
    sequence.setMinOccurs(ZERO);
    sequence.setMaxOccurs(MAX_ONE);

    builder.addInfrastructureParameters(providerType, providerModel, sequence);
    providerModel.getParameterGroupModels().forEach(group -> {
      if (!group.isShowInDsl()) {
        builder.addParameterToSequence(builder.registerParameters(providerType, group.getParameterModels()), sequence);
      } else {
        builder.addInlineParameterGroup(group, sequence);
      }
    });

    providerType.setSequence(sequence);

  }
}
