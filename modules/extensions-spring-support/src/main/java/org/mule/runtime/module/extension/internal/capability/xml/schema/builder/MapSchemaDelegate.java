/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_KEY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.xml.SchemaConstants;

import java.math.BigInteger;

/**
 * Builder delegation class to generate an XSD schema that describes an {@link DictionaryType}
 *
 * @since 4.0.0
 */
final class MapSchemaDelegate {

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;

  MapSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void generateMapElement(DictionaryType metadataType, DslElementSyntax paramDsl, String description, boolean required,
                          ExplicitGroup all) {
    BigInteger minOccurs = required ? ONE : ZERO;
    LocalComplexType mapComplexType = generateMapComplexType(paramDsl, metadataType);

    TopLevelElement mapElement = builder.createTopLevelElement(paramDsl.getElementName(), minOccurs, "1");
    mapElement.setAnnotation(builder.createDocAnnotation(description));
    all.getParticle().add(objectFactory.createElement(mapElement));

    mapElement.setComplexType(mapComplexType);
  }

  private LocalComplexType generateMapComplexType(DslElementSyntax mapDsl, final DictionaryType metadataType) {
    final MetadataType keyType = metadataType.getKeyType();
    final MetadataType valueType = metadataType.getValueType();
    final LocalComplexType entryComplexType = new LocalComplexType();
    final Attribute keyAttribute = builder.createAttribute(ATTRIBUTE_NAME_KEY, keyType, true, REQUIRED);
    entryComplexType.getAttributeOrAttributeGroup().add(keyAttribute);

    final LocalComplexType mapComplexType = new LocalComplexType();
    final ExplicitGroup mapEntrySequence = new ExplicitGroup();
    mapComplexType.setSequence(mapEntrySequence);

    DslElementSyntax entryValueDsl = mapDsl.getGeneric(valueType)
        .orElseThrow(() -> new IllegalArgumentException("Illegal DslSyntax definition of the given DictionaryType. The DslElementSyntax for the entry is required"));

    final TopLevelElement mapEntryElement = new TopLevelElement();
    mapEntryElement.setName(entryValueDsl.getElementName());
    mapEntryElement.setMinOccurs(ZERO);
    mapEntryElement.setMaxOccurs(SchemaConstants.UNBOUNDED);

    valueType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        final boolean shouldGenerateChildElement = entryValueDsl.supportsChildDeclaration();

        entryComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(ATTRIBUTE_NAME_VALUE, valueType, !shouldGenerateChildElement, SUPPORTED));

        if (shouldGenerateChildElement) {
          ExplicitGroup singleItemSequence = new ExplicitGroup();
          singleItemSequence.setMaxOccurs("1");

          TopLevelElement mapItemElement = builder.createTypeRef(objectType, false);
          singleItemSequence.getParticle().add(objectFactory.createElement(mapItemElement));

          entryComplexType.setSequence(singleItemSequence);
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        entryComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(ATTRIBUTE_NAME_VALUE, valueType, false, SUPPORTED));
        entryComplexType.setSequence(new ExplicitGroup());

        LocalComplexType itemComplexType = new LocalComplexType();
        MetadataType itemType = arrayType.getType();
        itemComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(ATTRIBUTE_NAME_VALUE, itemType, true, REQUIRED));

        DslElementSyntax itemDsl = entryValueDsl.getGeneric(itemType)
            .orElseThrow(() -> new IllegalArgumentException("Illegal DslSyntax definition of the given ArrayType. The DslElementSyntax for the item is required"));

        TopLevelElement itemElement =
            builder.createTopLevelElement(itemDsl.getElementName(), ZERO, SchemaConstants.UNBOUNDED, itemComplexType);
        entryComplexType.getSequence().getParticle().add(objectFactory.createElement(itemElement));
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        entryComplexType.getAttributeOrAttributeGroup().add(builder.createValueAttribute(valueType));
      }
    });

    mapEntryElement.setComplexType(entryComplexType);

    mapEntrySequence.getParticle().add(objectFactory.createElement(mapEntryElement));

    return mapComplexType;
  }

}
