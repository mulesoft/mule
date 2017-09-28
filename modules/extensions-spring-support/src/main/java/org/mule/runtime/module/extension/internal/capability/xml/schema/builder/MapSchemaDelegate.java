/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.lang.String.format;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.UNBOUNDED;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * Builder delegation class to generate an XSD schema that describes an open {@link ObjectType}
 *
 * @since 4.0.0
 */
final class MapSchemaDelegate {

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;
  private final MetadataType keyType;

  MapSchemaDelegate(SchemaBuilder builder, ClassTypeLoader typeLoader) {
    this.builder = builder;
    this.keyType = typeLoader.load(String.class);
  }

  void generateMapElement(ObjectType metadataType, DslElementSyntax paramDsl, String description, boolean required,
                          List<TopLevelElement> all) {
    BigInteger minOccurs = required ? ONE : ZERO;
    LocalComplexType mapComplexType = generateMapComplexType(paramDsl, metadataType);

    TopLevelElement mapElement = builder.createTopLevelElement(paramDsl.getElementName(), minOccurs, MAX_ONE);
    mapElement.setAnnotation(builder.createDocAnnotation(description));
    mapElement.setComplexType(mapComplexType);

    all.add(mapElement);
  }

  private LocalComplexType generateMapComplexType(DslElementSyntax mapDsl, final ObjectType metadataType) {
    final MetadataType valueType = metadataType.getOpenRestriction().get();
    final LocalComplexType entryComplexType = new LocalComplexType();
    final Attribute keyAttribute = builder.createAttribute(KEY_ATTRIBUTE_NAME, keyType, true, REQUIRED);
    entryComplexType.getAttributeOrAttributeGroup().add(keyAttribute);

    final LocalComplexType mapComplexType = new LocalComplexType();
    final ExplicitGroup mapEntrySequence = new ExplicitGroup();
    mapComplexType.setSequence(mapEntrySequence);

    DslElementSyntax entryValueDsl = mapDsl.getGeneric(valueType)
        .orElseThrow(() -> new IllegalArgumentException("Illegal DslSyntax definition of the given DictionaryType. The DslElementSyntax for the entry is required"));

    final TopLevelElement mapEntryElement = new TopLevelElement();
    mapEntryElement.setName(entryValueDsl.getElementName());
    mapEntryElement.setMinOccurs(ZERO);
    mapEntryElement.setMaxOccurs(UNBOUNDED);

    valueType.accept(new MetadataTypeVisitor() {

      /**
       * For a Map with an {@link ObjectType} as value. The resulting {@link ComplexType} declares a sequence of either a
       * {@code ref} or a {@code choice}.
       * <p/>
       * It creates an element {@code ref} to the concrete element whose {@code type} is the {@link ComplexType} associated to the
       * {@code objectType}
       * <p/>
       * In the case of having a {@link DslElementSyntax#isWrapped wrapped} {@link ObjectType}, then a {@link ExplicitGroup
       * Choice} group that can receive a {@code ref} to any subtype that this wrapped type might have, be it either a top-level
       * element for the mule schema, or if it can only be declared as child of this element.
       *
       * If the map's value is another map, then a value attribute is created for the value map.
       * 
       * @param objectType the item's type
       */
      @Override
      public void visitObject(ObjectType objectType) {
        Optional<DslElementSyntax> containedElement = entryValueDsl.getContainedElement(VALUE_ATTRIBUTE_NAME);
        if (isMap(objectType) || !containedElement.isPresent()) {
          defaultVisit(objectType);
          return;
        }

        final boolean shouldGenerateChildElement =
            containedElement.get().supportsChildDeclaration() || containedElement.get().isWrapped();

        entryComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(VALUE_ATTRIBUTE_NAME, valueType, !shouldGenerateChildElement, SUPPORTED));

        if (shouldGenerateChildElement) {
          DslElementSyntax typeDsl = builder.getDslResolver().resolve(objectType)
              .orElseThrow(() -> new IllegalArgumentException(format("The given type [%s] cannot be represented as a child element in Map entries",
                                                                     getId(objectType))));
          if (typeDsl.isWrapped()) {
            ExplicitGroup choice = builder.createTypeRefChoiceLocalOrGlobal(typeDsl, objectType, ZERO, UNBOUNDED);
            entryComplexType.setChoice(choice);

          } else {
            ExplicitGroup singleItemSequence = new ExplicitGroup();
            singleItemSequence.setMaxOccurs("1");
            TopLevelElement mapItemElement = builder.createTypeRef(typeDsl, objectType, false);
            singleItemSequence.getParticle().add(objectFactory.createElement(mapItemElement));

            entryComplexType.setSequence(singleItemSequence);
          }
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        entryComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(VALUE_ATTRIBUTE_NAME, valueType, false, SUPPORTED));
        entryComplexType.setSequence(new ExplicitGroup());

        LocalComplexType itemComplexType = new LocalComplexType();
        MetadataType itemType = arrayType.getType();
        itemComplexType.getAttributeOrAttributeGroup()
            .add(builder.createAttribute(VALUE_ATTRIBUTE_NAME, itemType, true, REQUIRED));

        DslElementSyntax itemDsl = entryValueDsl.getGeneric(itemType)
            .orElseThrow(() -> new IllegalArgumentException("Illegal DslSyntax definition of the given ArrayType. The DslElementSyntax for the item is required"));

        TopLevelElement itemElement =
            builder.createTopLevelElement(itemDsl.getElementName(), ZERO, UNBOUNDED, itemComplexType);
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
