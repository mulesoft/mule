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
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.UNBOUNDED;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.util.List;

/**
 * Builder delegation class to generate an XSD schema that describes an {@link ArrayType}
 *
 * @since 4.0.0
 */
class CollectionSchemaDelegate {

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;

  CollectionSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void generateCollectionElement(ArrayType metadataType, DslElementSyntax collectionDsl, String description, boolean required,
                                 List<TopLevelElement> all) {
    LocalComplexType collectionComplexType = generateCollectionComplexType(collectionDsl, metadataType);

    TopLevelElement collectionElement = builder.createTopLevelElement(collectionDsl.getElementName(),
                                                                      required ? ONE : ZERO,
                                                                      MAX_ONE);
    collectionElement.setAnnotation(builder.createDocAnnotation(description));
    collectionElement.setComplexType(collectionComplexType);

    all.add(collectionElement);
  }

  private LocalComplexType generateCollectionComplexType(DslElementSyntax collectionDsl, final ArrayType metadataType) {
    final LocalComplexType collectionComplexType = new LocalComplexType();
    final ExplicitGroup sequence = new ExplicitGroup();

    final MetadataType genericType = metadataType.getType();
    DslElementSyntax itemDsl = collectionDsl.getGeneric(genericType)
        .orElseThrow(() -> new IllegalArgumentException(format("Missing item's DSL information for collection [%s]",
                                                               collectionDsl.getAttributeName())));
    genericType.accept(new MetadataTypeVisitor() {

      /**
       * For a Collection with an {@link ObjectType} as generic. The generated {@link ComplexType} declares a sequence of either a
       * {@code ref} or a {@code choice}.
       * <p/>
       * It creates an element {@code ref} to the concrete element whose {@code type} is the {@link ComplexType} associated to the
       * {@code objectType}
       * <p/>
       * In the case of having a {@link DslElementSyntax#isWrapped wrapped} {@link ObjectType}, then a {@link ExplicitGroup
       * Choice} group that can receive a {@code ref} to any subtype that this wrapped type might have, be it either a top-level
       * element for the mule schema, or if it can only be declared as child of this element.
       *
       * If the collections's value is a map, then a value attribute is created for the value map.
       * 
       * @param objectType the item's type
       */
      @Override
      public void visitObject(ObjectType objectType) {
        if (isMap(objectType)) {
          defaultVisit(objectType);
          return;
        }

        DslElementSyntax typeDsl = builder.getDslResolver().resolve(objectType)
            .orElseThrow(() -> new IllegalArgumentException(format("The given type [%s] cannot be represented as a collection item",
                                                                   getId(objectType))));

        if (typeDsl.isWrapped()) {
          ExplicitGroup choice = builder.createTypeRefChoiceLocalOrGlobal(typeDsl, objectType, ZERO, UNBOUNDED);
          sequence.getParticle().add(objectFactory.createChoice(choice));
        } else {
          TopLevelElement collectionItemElement = builder.createTypeRef(typeDsl, objectType, false);
          collectionItemElement.setMaxOccurs(UNBOUNDED);
          sequence.getParticle().add(objectFactory.createElement(collectionItemElement));
        }
      }

      /**
       * For a Collection with any other type as generic.
       * The generated {@link ComplexType} declares a sequence of child elements with an inline declaration of the type
       *
       * @param metadataType the item's type
       */
      @Override
      protected void defaultVisit(MetadataType metadataType) {
        final LocalComplexType complexType = new LocalComplexType();
        complexType.getAttributeOrAttributeGroup().add(builder.createValueAttribute(genericType));


        TopLevelElement collectionItemElement =
            builder.createTopLevelElement(itemDsl.getElementName(), ZERO, UNBOUNDED);
        collectionItemElement.setComplexType(complexType);
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

      }
    });

    collectionComplexType.setSequence(sequence);
    return collectionComplexType;
  }

}
