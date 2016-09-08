/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.metadata.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils.deriveModelProperties;
import static org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.UNBOUNDED;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.introspection.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.property.XmlHintsModelProperty;
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
import java.util.Optional;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate an XSD schema that describes an {@link ObjectType}
 *
 * @since 4.0.0
 */
final class ObjectTypeSchemaDelegate {

  private final Map<String, ComplexTypeHolder> registeredComplexTypesHolders = new LinkedHashMap<>();
  private final Map<String, TopLevelElement> registeredGlobalElementTypes = new LinkedHashMap<>();
  private final ObjectFactory objectFactory = new ObjectFactory();
  private final SchemaBuilder builder;

  ObjectTypeSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
  }

  void generatePojoElement(ObjectType objectType, DslElementSyntax paramDsl, Optional<XmlHintsModelProperty> paramXmlHints,
                           String description, ExplicitGroup all) {

    if (paramDsl.supportsChildDeclaration()) {
      if (isImported(objectType)) {
        addImportedTypeElement(paramDsl, description, objectType, all);
      } else {
        if (paramDsl.isWrapped()) {
          declareRefToType(objectType, paramDsl, description, all);
        } else {
          declareTypeInline(objectType, paramDsl, description, all);
        }
      }
    }

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(objectType);
    boolean allowsRef = paramXmlHints.map(XmlHintsModelProperty::allowsReferences).orElse(true);
    if (allowsRef && typeDsl.supportsTopLevelDeclaration() && !isImported(objectType)) {
      // We need to register the type, just in case people want to use it as global elements
      registerPojoType(objectType, description);
    }
  }

  private boolean isImported(MetadataType type) {
    return builder.getImportedTypes().get(type) != null;
  }

  private void declareTypeInline(ObjectType objectType, DslElementSyntax paramDsl, String description, ExplicitGroup all) {
    registerPojoComplexType(objectType, null, description);
    String typeName = getBaseTypeName(objectType);
    QName localQName = new QName(paramDsl.getNamespaceUri(), typeName, paramDsl.getNamespace());
    addChildElementTypeExtension(localQName, description, paramDsl.getElementName(), all);
  }

  private void declareRefToType(ObjectType objectType, DslElementSyntax paramDsl, String description, ExplicitGroup all) {
    registerPojoSubtypes(objectType, builder.getSubTypesMapping().getSubTypes(objectType));
    addAbstractTypeRef(paramDsl, description, objectType, all);
  }

  private void addChildElementTypeExtension(QName base, String description, String name, ExplicitGroup all) {
    TopLevelElement objectElement = builder.createTopLevelElement(name, ZERO, "1");
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createTypeExtension(base));
    all.getParticle().add(objectFactory.createElement(objectElement));
  }

  private void addImportedTypeElement(DslElementSyntax paramDsl, String description, MetadataType metadataType,
                                      ExplicitGroup all) {

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(metadataType);
    if (paramDsl.isWrapped()) {

      TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), ZERO, "1");
      objectElement.setComplexType(new LocalComplexType());
      objectElement.setAnnotation(builder.createDocAnnotation(description));

      if (typeDsl.isWrapped()) {
        objectElement.getComplexType().setSequence(builder.createTypeRefChoiceLocalOrGlobal(metadataType, ZERO, UNBOUNDED));

      } else {
        ExplicitGroup sequence = new ExplicitGroup();
        sequence.setMinOccurs(ONE);
        sequence.setMaxOccurs("1");

        QName refQName = new QName(paramDsl.getNamespaceUri(), typeDsl.getAbstractElementName(), paramDsl.getNamespace());
        sequence.getParticle().add(objectFactory.createElement(builder.createRefElement(refQName, false)));
        objectElement.getComplexType().setSequence(sequence);
      }

      all.getParticle().add(objectFactory.createElement(objectElement));

    } else {
      QName extensionBase = new QName(typeDsl.getNamespaceUri(), sanitizeName(getId(metadataType)), typeDsl.getNamespace());
      addChildElementTypeExtension(extensionBase, description, paramDsl.getElementName(), all);
    }
  }

  private void addAbstractTypeRef(DslElementSyntax paramDsl, String description, MetadataType metadataType, ExplicitGroup all) {
    TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), ZERO, "1");
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createComplexTypeWithAbstractElementRef(metadataType));

    all.getParticle().add(objectFactory.createElement(objectElement));
  }


  private LocalComplexType createComplexTypeWithAbstractElementRef(MetadataType metadataType) {

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(metadataType);

    LocalComplexType complexType = new LocalComplexType();
    if (typeDsl.isWrapped()) {
      complexType.setChoice(builder.createTypeRefChoiceLocalOrGlobal(metadataType, ONE, "1"));
    } else {
      ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ONE);
      sequence.setMaxOccurs("1");

      sequence.getParticle().add(objectFactory.createElement(createRefToLocalElement(typeDsl, metadataType)));
      complexType.setSequence(sequence);
    }

    return complexType;
  }

  private TopLevelElement createRefToLocalElement(DslElementSyntax typeDsl, MetadataType metadataType) {
    registerPojoType(metadataType, EMPTY);

    QName qName = new QName(typeDsl.getNamespaceUri(), typeDsl.getAbstractElementName(), typeDsl.getNamespace());
    return builder.createRefElement(qName, false);
  }

  String registerPojoType(MetadataType metadataType, String description) {
    return registerPojoType(metadataType, null, description);
  }

  /**
   * Registers a pojo type creating a base complex type and a substitutable top level type while assigning it a name. This method
   * will not register the same type twice even if requested to
   *
   * @param type a {@link MetadataType} describing a pojo type
   * @param baseType     a {@link MetadataType} describing a pojo's base type
   * @param description  the type's description
   * @return the reference name of the complexType
   */
  private String registerPojoType(MetadataType type, MetadataType baseType, String description) {
    if (!isImported(type)) {
      DslElementSyntax typeDsl = builder.getDslResolver().resolve(type);
      registerPojoComplexType((ObjectType) type, (ObjectType) baseType, description);

      if (typeDsl.supportsTopLevelDeclaration() || typeDsl.isWrapped() ||
          !builder.getSubTypesMapping().getSuperTypes(type).isEmpty()) {

        registerPojoGlobalElements(typeDsl, (ObjectType) type, (ObjectType) baseType, description);
      }
    }

    return getBaseTypeName(type);
  }

  private ComplexType registerPojoComplexType(ObjectType type, ObjectType baseType, String description) {
    String typeId = getId(type);
    if (registeredComplexTypesHolders.get(typeId) != null) {
      return registeredComplexTypesHolders.get(typeId).getComplexType();
    }

    QName base = getComplexTypeBase(baseType);
    Collection<ObjectFieldType> fields;
    if (baseType == null) {
      fields = type.getFields();
    } else {
      fields = type.getFields().stream()
          .filter(field -> !baseType.getFields().stream()
              .anyMatch(other -> other.getKey().getName().getLocalPart().equals(field.getKey().getName().getLocalPart())))
          .collect(toList());
    }

    ComplexType complexType = declarePojoAsType(type, base, description, fields);
    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(complexType);
    return complexType;
  }

  private QName getComplexTypeBase(ObjectType baseType) {
    if (baseType == null) {
      return MULE_ABSTRACT_EXTENSION_TYPE;
    }
    DslElementSyntax baseDsl = builder.getDslResolver().resolve(baseType);
    return new QName(baseDsl.getNamespaceUri(), getBaseTypeName(baseType), baseDsl.getNamespace());
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

      if (isParameterGroupAtPojoLevel(field)) {
        ((ObjectType) field.getValue()).getFields().forEach(subField -> declareObjectField(subField, extension, all));
      } else {
        declareObjectField(field, extension, all);
      }

      if (all.getParticle().isEmpty()) {
        extension.setSequence(null);
      }
    }

    return complexType;
  }

  private boolean isParameterGroupAtPojoLevel(ObjectFieldType field) {
    return field.getValue() instanceof ObjectType && field.getAnnotation(FlattenedTypeAnnotation.class).isPresent();
  }

  private void declareObjectField(ObjectFieldType field, ExtensionType extension, ExplicitGroup all) {
    field.getValue().accept(builder.getParameterDeclarationVisitor(extension, all, asParameter(field)));
  }

  private void registerPojoGlobalElements(DslElementSyntax typeDsl, ObjectType type, ObjectType baseType, String description) {

    if (registeredGlobalElementTypes.containsKey(globalTypeKey(typeDsl))) {
      return;
    }

    registerPojoComplexType(type, baseType, description);
    QName typeQName = getTypeQName(typeDsl, type);
    TopLevelElement abstractElement = registerAbstractElement(typeQName, typeDsl, baseType);
    if (typeDsl.supportsTopLevelDeclaration() || (typeDsl.supportsChildDeclaration() && typeDsl.isWrapped()) ||
        !builder.getSubTypesMapping().getSuperTypes(type).isEmpty()) {
      registerConcreteGlobalElement(typeDsl, description, abstractElement.getName(), typeQName);
    }
  }

  QName getTypeQName(DslElementSyntax typeDsl, MetadataType type) {
    return new QName(builder.getSchema().getTargetNamespace(), getBaseTypeName(type), typeDsl.getNamespace());
  }

  TopLevelElement registerAbstractElement(MetadataType type, DslElementSyntax typeDsl) {
    return registerAbstractElement(getTypeQName(typeDsl, type), typeDsl, null);
  }

  private TopLevelElement registerAbstractElement(QName typeQName, DslElementSyntax typeDsl, ObjectType baseType) {
    TopLevelElement element = registeredGlobalElementTypes.get(typeDsl.getNamespace() + typeDsl.getAbstractElementName());
    if (element != null) {
      return element;
    }

    DslElementSyntax baseDsl = baseType != null ? builder.getDslResolver().resolve(baseType) : null;
    if (typeDsl.isWrapped()) {
      createGlobalMuleExtensionAbstractElement(typeQName, typeDsl, baseDsl);
    }

    TopLevelElement abstractElement = new TopLevelElement();
    abstractElement.setName(typeDsl.getAbstractElementName());
    abstractElement.setAbstract(true);
    if (!typeDsl.supportsTopLevelDeclaration()) {
      abstractElement.setType(typeQName);
    }

    if (baseDsl != null || typeDsl.supportsTopLevelDeclaration()) {
      QName substitutionGroup = getAbstractElementSubstitutionGroup(typeDsl, baseDsl);
      abstractElement.setSubstitutionGroup(substitutionGroup);
    }

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(abstractElement);
    registeredGlobalElementTypes.put(typeDsl.getNamespace() + typeDsl.getAbstractElementName(), abstractElement);

    return abstractElement;
  }

  private QName getAbstractElementSubstitutionGroup(DslElementSyntax typeDsl, DslElementSyntax baseDsl) {
    QName substitutionGroup;
    if (baseDsl != null) {
      String abstractElementName = typeDsl.supportsTopLevelDeclaration() ? getGlobalAbstractName(baseDsl)
          : baseDsl.getAbstractElementName();

      substitutionGroup = new QName(baseDsl.getNamespaceUri(), abstractElementName, baseDsl.getNamespace());

    } else {
      if (typeDsl.isWrapped()) {
        substitutionGroup = new QName(typeDsl.getNamespaceUri(), getGlobalAbstractName(typeDsl), typeDsl.getNamespace());
      } else {
        substitutionGroup = MULE_ABSTRACT_EXTENSION;
      }
    }
    return substitutionGroup;
  }

  private void createGlobalMuleExtensionAbstractElement(QName typeQName, DslElementSyntax typeDsl, DslElementSyntax baseDsl) {
    QName globalSubGroup;
    if (baseDsl != null) {
      globalSubGroup = new QName(baseDsl.getNamespaceUri(), getGlobalAbstractName(baseDsl), baseDsl.getNamespace());
    } else {
      globalSubGroup = MULE_ABSTRACT_EXTENSION;
    }

    TopLevelElement abstractElement = new TopLevelElement();
    abstractElement.setName(getGlobalAbstractName(typeDsl));
    abstractElement.setSubstitutionGroup(globalSubGroup);
    abstractElement.setAbstract(true);

    if (!typeDsl.supportsTopLevelDeclaration()) {
      abstractElement.setType(typeQName);
    }

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(abstractElement);
  }

  private String getGlobalAbstractName(DslElementSyntax dsl) {
    return "global-" + dsl.getAbstractElementName();
  }

  void registerConcreteGlobalElement(DslElementSyntax typeDsl, String description,
                                     String abstractElementName, QName typeQName) {

    if (registeredGlobalElementTypes.containsKey(globalTypeKey(typeDsl))) {
      return;
    }

    TopLevelElement objectElement = new TopLevelElement();
    objectElement.setName(typeDsl.getElementName());

    objectElement.setSubstitutionGroup(new QName(typeDsl.getNamespaceUri(), abstractElementName, typeDsl.getNamespace()));
    objectElement.setAnnotation(builder.createDocAnnotation(description));

    objectElement.setComplexType(createTypeExtension(typeQName));
    if (typeDsl.supportsTopLevelDeclaration()) {
      objectElement.getComplexType().getComplexContent().getExtension().getAttributeOrAttributeGroup()
          .add(builder.createNameAttribute(false));
    }

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(objectElement);

    registeredGlobalElementTypes.put(globalTypeKey(typeDsl), objectElement);
  }

  private String globalTypeKey(DslElementSyntax typeDsl) {
    return typeDsl.getNamespace() + typeDsl.getElementName();
  }

  private ImmutableParameterModel asParameter(ObjectFieldType field) {
    return new ImmutableParameterModel(field.getKey().getName().getLocalPart(), "", field.getValue(), false, field.isRequired(),
                                       getExpressionSupport(field), getDefaultValue(field).orElse(null),
                                       deriveModelProperties(field));
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
