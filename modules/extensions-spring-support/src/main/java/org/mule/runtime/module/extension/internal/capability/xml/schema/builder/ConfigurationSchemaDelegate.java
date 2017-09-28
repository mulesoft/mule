/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_SHARED_EXTENSION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getFirstImplicit;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.util.List;
import java.util.Optional;

/**
 * Builder delegation class to generate a XSD schema that describes a {@link ConfigurationModel}
 *
 * @since 4.0.0
 */
final class ConfigurationSchemaDelegate {

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;
  private Schema schema;

  ConfigurationSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void registerConfigElement(Schema schema, final ConfigurationModel configurationModel,
                             DslElementSyntax dslConfigElement) {
    this.schema = schema;

    ExtensionType config = registerExtension(dslConfigElement.getElementName());
    config.getAttributeOrAttributeGroup().add(builder.createNameAttribute(true));
    config.setAnnotation(builder.createDocAnnotation(configurationModel.getDescription()));

    Optional<TopLevelElement> connectionElement = addConnectionProviderElement(configurationModel);

    if (connectionElement.isPresent() || !configurationModel.getParameterGroupModels().isEmpty()) {
      final ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ZERO);
      sequence.setMaxOccurs(MAX_ONE);

      connectionElement.ifPresent(connection -> {
        sequence.getParticle().add(objectFactory.createElement(connection));
        if (builder.isRequired(connection)) {
          sequence.setMinOccurs(ONE);
        }
      });

      builder.addInfrastructureParameters(config, configurationModel, sequence);
      configurationModel.getParameterGroupModels().forEach(group -> {
        if (!group.isShowInDsl()) {
          List<TopLevelElement> parameters = builder.registerParameters(config, group.getParameterModels());
          builder.addParameterToSequence(parameters, sequence);
        } else {
          builder.addInlineParameterGroup(group, sequence);
        }
      });

      config.setSequence(sequence);
    }
  }

  private ExtensionType registerExtension(String name) {
    LocalComplexType complexType = new LocalComplexType();

    Element extension = new TopLevelElement();
    extension.setName(name);
    extension.setSubstitutionGroup(MULE_ABSTRACT_SHARED_EXTENSION);
    extension.setComplexType(complexType);

    ComplexContent complexContent = new ComplexContent();
    complexType.setComplexContent(complexContent);
    ExtensionType complexContentExtension = new ExtensionType();
    complexContentExtension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
    complexContent.setExtension(complexContentExtension);

    schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);

    return complexContentExtension;
  }

  private Optional<TopLevelElement> addConnectionProviderElement(ConfigurationModel configurationModel) {
    ExtensionModel extensionModel = builder.getExtensionModel();
    if (!extensionModel.getConnectionProviders().isEmpty() || !configurationModel.getConnectionProviders().isEmpty()) {
      TopLevelElement objectElement = new TopLevelElement();

      boolean hasImplicitConnection = getFirstImplicit(extensionModel.getConnectionProviders()) != null
          || getFirstImplicit(configurationModel.getConnectionProviders()) != null;

      objectElement.setMinOccurs(hasImplicitConnection ? ZERO : ONE);
      objectElement.setMaxOccurs(MAX_ONE);
      objectElement.setRef(MULE_CONNECTION_PROVIDER_ELEMENT);

      return Optional.of(objectElement);
    }
    return empty();
  }
}
