/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.metadata.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.extension.api.introspection.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate an XSD schema that describes an {@link ObjectType}
 *
 * @since 4.0.0
 */
final class ObjectTypeSchemaDelegate {

  private final Map<String, ComplexTypeHolder> registeredComplexTypesHolders = new LinkedHashMap<>();
  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;

  ObjectTypeSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void generatePojoElement(ObjectType objectType, DslElementSyntax paramDsl, String name, String description, ExplicitGroup all) {

    if (!paramDsl.supportsChildDeclaration()) {
      DslElementSyntax typeDsl = builder.getDslResolver().resolve(objectType);
      if (typeDsl.supportsChildDeclaration()) {
        // We need to register the type, just in case people want to use it as global elements
        registerPojoType(objectType, description);
      }
    } else if (builder.getImportedTypes().get(objectType) != null) {
      addImportedTypeElement(paramDsl, name, description, objectType, all);
    } else {
      if (paramDsl.isWrapped()) {
        registerPojoSubtypes(objectType, builder.getSubTypesMapping().getSubTypes(objectType));
        addAbstractTypeRef(paramDsl, description, objectType, all);
      } else {
        String typeName = registerPojoType(objectType, description);
        QName localQName = new QName(paramDsl.getNamespaceUri(), typeName, paramDsl.getNamespace());
        addChildElementTypeExtension(localQName, description, name, all);
      }
    }
  }

  private void addChildElementTypeExtension(QName base, String description, String name, ExplicitGroup all) {
    TopLevelElement objectElement = builder.createTopLevelElement(hyphenize(name), ZERO, "1");
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createTypeExtension(base));
    all.getParticle().add(objectFactory.createElement(objectElement));
  }

  private void addImportedTypeElement(DslElementSyntax paramDsl, String name, String description, MetadataType metadataType,
                                      ExplicitGroup all) {

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(metadataType);
    if (paramDsl.isWrapped()) {
      QName refQName = new QName(paramDsl.getNamespaceUri(), builder.getTopLevelAbstractName(typeDsl), paramDsl.getNamespace());

      TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), ZERO, "1");
      objectElement.setComplexType(new LocalComplexType());
      objectElement.setAnnotation(builder.createDocAnnotation(description));

      ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ONE);
      sequence.setMaxOccurs("1");
      sequence.getParticle().add(objectFactory.createElement(builder.createRefElement(refQName, false)));

      objectElement.getComplexType().setSequence(sequence);
      all.getParticle().add(objectFactory.createElement(objectElement));
    } else {
      QName extensionBase = new QName(typeDsl.getNamespaceUri(), sanitizeName(getId(metadataType)), typeDsl.getNamespace());
      addChildElementTypeExtension(extensionBase, description, name, all);
    }
  }

  private void addAbstractTypeRef(DslElementSyntax paramDsl, String description, MetadataType metadataType, ExplicitGroup all) {
    TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), ZERO, "1");
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createComplexTypeWithAbstractElementRef(metadataType));

    all.getParticle().add(objectFactory.createElement(objectElement));
  }


  private LocalComplexType createComplexTypeWithAbstractElementRef(MetadataType metadataType) {
    ExplicitGroup sequence = new ExplicitGroup();
    sequence.setMinOccurs(ONE);
    sequence.setMaxOccurs("1");

    sequence.getParticle().add(objectFactory.createElement(createRefToLocalElement(metadataType)));

    LocalComplexType complexType = new LocalComplexType();
    complexType.setSequence(sequence);

    return complexType;
  }

  private TopLevelElement createRefToLocalElement(MetadataType metadataType) {
    registerPojoType(metadataType, EMPTY);

    DslElementSyntax dsl = builder.getDslResolver().resolve(metadataType);

    QName qName = new QName(dsl.getNamespaceUri(), builder.getTopLevelAbstractName(dsl), dsl.getNamespace());
    return builder.createRefElement(qName, false);
  }

  String registerPojoType(MetadataType metadataType, String description) {
    return registerPojoType(metadataType, null, description);
  }

  /**
   * Registers a pojo type creating a base complex type and a substitutable top level type while assigning it a name. This method
   * will not register the same type twice even if requested to
   *
   * @param metadataType a {@link MetadataType} describing a pojo type
   * @param baseType a {@link MetadataType} describing a pojo's base type
   * @param description the type's description
   * @return the reference name of the complexType
   */
  private String registerPojoType(MetadataType metadataType, MetadataType baseType, String description) {
    ComplexTypeHolder alreadyRegisteredType = registeredComplexTypesHolders.get(getId(metadataType));
    if (alreadyRegisteredType != null) {
      return alreadyRegisteredType.getComplexType().getName();
    }

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(metadataType);
    registerPojoAsTypeWithBase((ObjectType) metadataType, (ObjectType) baseType, description);
    registerPojoGlobalElement(typeDsl, (ObjectType) metadataType, (ObjectType) baseType, description);

    return getBaseTypeName(metadataType);
  }

  private ComplexType registerPojoAsTypeWithBase(ObjectType metadataType, ObjectType baseType, String description) {
    String typeId = getId(metadataType);
    if (registeredComplexTypesHolders.get(typeId) != null) {
      return registeredComplexTypesHolders.get(typeId).getComplexType();
    }

    QName base;
    Collection<ObjectFieldType> fields;
    if (baseType == null) {
      base = MULE_ABSTRACT_EXTENSION_TYPE;
      fields = metadataType.getFields();
    } else {
      DslElementSyntax baseDsl = builder.getDslResolver().resolve(baseType);
      base = new QName(baseDsl.getNamespaceUri(), getBaseTypeName(baseType), baseDsl.getNamespace());
      fields = metadataType.getFields().stream().filter(f -> !baseType.getFields().contains(f)).collect(toList());
    }

    ComplexType complexType = declarePojoAsType(metadataType, base, description, fields);
    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(complexType);
    return complexType;
  }

  private ComplexType declarePojoAsType(ObjectType metadataType, QName base, String description,
                                        Collection<ObjectFieldType> fields) {
    final TopLevelComplexType complexType = new TopLevelComplexType();
    registeredComplexTypesHolders.put(getId(metadataType), new ComplexTypeHolder(complexType, metadataType));

    complexType.setName(sanitizeName(getId(metadataType)));
    complexType.setAnnotation(builder.createDocAnnotation(description));

    ComplexContent complexContent = new ComplexContent();
    complexType.setComplexContent(complexContent);

    final ExtensionType extension = new ExtensionType();
    extension.setBase(base);

    complexContent.setExtension(extension);

    for (ObjectFieldType field : fields) {
      final ExplicitGroup all = getOrCreateSequenceGroup(extension);

      field.getValue().accept(builder.getParameterDeclarationVisitor(extension, all, asParameter(field)));

      if (all.getParticle().isEmpty()) {
        extension.setSequence(null);
      }
    }

    return complexType;
  }

  private void registerPojoGlobalElement(DslElementSyntax typeDsl, ObjectType metadataType, ObjectType baseType,
                                         String description) {
    String abstractElementName = builder.getTopLevelAbstractName(typeDsl);

    TopLevelElement abstractElement = new TopLevelElement();
    abstractElement.setName(abstractElementName);


    QName substitutionGroup = MULE_ABSTRACT_EXTENSION;
    if (baseType != null) {
      DslElementSyntax baseDsl = builder.getDslResolver().resolve(baseType);
      substitutionGroup = new QName(baseDsl.getNamespaceUri(), builder.getTopLevelAbstractName(baseDsl), baseDsl.getNamespace());
    }

    abstractElement.setSubstitutionGroup(substitutionGroup);
    abstractElement.setAbstract(true);

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(abstractElement);

    QName typeQName =
        new QName(builder.getSchema().getTargetNamespace(), registerPojoType(metadataType, description), typeDsl.getNamespace());
    if (!typeDsl.supportsTopLevelDeclaration()) {
      abstractElement.setType(typeQName);
    } else {
      TopLevelElement objectElement = new TopLevelElement();
      objectElement.setName(getTopLevelTypeName(metadataType));
      objectElement.setSubstitutionGroup(new QName(typeDsl.getNamespaceUri(), abstractElementName, typeDsl.getNamespace()));
      objectElement.setAnnotation(builder.createDocAnnotation(description));

      objectElement.setComplexType(createTypeExtension(typeQName));
      objectElement.getComplexType().getComplexContent().getExtension().getAttributeOrAttributeGroup()
          .add(builder.createNameAttribute(false));
      builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(objectElement);
    }
  }

  private ImmutableParameterModel asParameter(ObjectFieldType field) {
    return new ImmutableParameterModel(field.getKey().getName().getLocalPart(), "", field.getValue(), false, field.isRequired(),
                                       getExpressionSupport(field), getDefaultValue(field).orElse(null), emptySet());
  }

  private ExplicitGroup getOrCreateSequenceGroup(ExtensionType extension) {
    ExplicitGroup all = extension.getSequence();
    if (all == null) {
      all = new ExplicitGroup();
      extension.setSequence(all);
    }
    return all;
  }

  void registerPojoSubtypes(MetadataType baseType, List<MetadataType> subTypes) {
    if (builder.getImportedTypes().get(baseType) == null) {
      registerPojoType(baseType, EMPTY);
    }

    subTypes.forEach(subtype -> registerPojoType(subtype, baseType, EMPTY));
  }

  private LocalComplexType createTypeExtension(QName base) {
    final LocalComplexType complexType = new LocalComplexType();
    ComplexContent complexContent = new ComplexContent();
    complexType.setComplexContent(complexContent);

    final ExtensionType extension = new ExtensionType();
    extension.setBase(base);

    complexContent.setExtension(extension);
    return complexType;
  }

  private String getBaseTypeName(MetadataType type) {
    return sanitizeName(getId(type));
  }

}
