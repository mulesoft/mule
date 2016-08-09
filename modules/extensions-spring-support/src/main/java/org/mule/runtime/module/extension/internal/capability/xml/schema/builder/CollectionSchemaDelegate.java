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
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.UNBOUNDED;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.xml.SchemaConstants;

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
                                 ExplicitGroup all) {
    LocalComplexType collectionComplexType = generateCollectionComplexType(collectionDsl, metadataType);

    TopLevelElement collectionElement = builder.createTopLevelElement(collectionDsl.getElementName(), required ? ONE : ZERO, "1");
    collectionElement.setAnnotation(builder.createDocAnnotation(description));
    all.getParticle().add(objectFactory.createElement(collectionElement));

    collectionElement.setComplexType(collectionComplexType);
  }

  private LocalComplexType generateCollectionComplexType(DslElementSyntax collectionDsl, final ArrayType metadataType) {
    final LocalComplexType collectionComplexType = new LocalComplexType();
    final ExplicitGroup sequence = new ExplicitGroup();

    final MetadataType genericType = metadataType.getType();
    DslElementSyntax itemDsl = collectionDsl.getGeneric(genericType)
        .orElseThrow(() -> new IllegalArgumentException(format("Missing item's DSL information for collection [%s]",
                                                               collectionDsl.getAttributeName())));
    genericType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        TopLevelElement collectionItemElement = builder.createTypeRef(objectType, false);
        collectionItemElement.setMaxOccurs(UNBOUNDED);
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        final LocalComplexType complexType = new LocalComplexType();
        complexType.getAttributeOrAttributeGroup().add(builder.createValueAttribute(genericType));


        TopLevelElement collectionItemElement =
            builder.createTopLevelElement(itemDsl.getElementName(), ZERO, SchemaConstants.UNBOUNDED);
        collectionItemElement.setComplexType(complexType);
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

      }
    });

    collectionComplexType.setSequence(sequence);
    return collectionComplexType;
  }

}
