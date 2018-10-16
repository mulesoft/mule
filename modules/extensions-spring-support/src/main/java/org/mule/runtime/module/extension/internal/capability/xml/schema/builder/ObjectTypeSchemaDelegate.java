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
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_SHARED_EXTENSION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.UNBOUNDED;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getBaseType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getLayoutModel;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getSubstitutionGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
  private final DslSyntaxResolver dsl;

  ObjectTypeSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
    this.dsl = builder.getDslResolver();
  }

  /**
   * For any given {@code parameter} with an {@link ObjectType} as {@link MetadataType}, the element generated in the schema will
   * vary depending on the properties of the type itself along with the properties associated to the parameter.
   * <p>
   * This method serves as a resolver for all that logic, creating the required element for the parameter with complex type.
   *  @param type the {@link ObjectType} of the parameter for which the element is being created
   * @param paramSyntax the {@link DslElementSyntax} of the parameter for which the element is being created
   * @param paramDsl the {@link ParameterDslConfiguration} associated to the parameter, if any is present.
   * @param description the documentation associated to the parameter
   * @param all the {@link ExplicitGroup group} the generated element should belong to
   * @param required whether or not the element should be required
   */
  void generatePojoElement(ObjectType type, DslElementSyntax paramSyntax, ParameterDslConfiguration paramDsl,
                           String description, List<TopLevelElement> all, boolean required) {

    if (paramSyntax.supportsChildDeclaration()) {
      if (builder.isImported(type)) {
        addImportedTypeElement(paramSyntax, description, type, all, required);
      } else {
        if (paramSyntax.isWrapped()) {
          declareRefToType(type, paramSyntax, description, all, required);
        } else if (getSubstitutionGroup(type).isPresent()) {
          declareRefToType(type, paramSyntax, description, all, required);
          registerAbstractElement(type, paramSyntax);
        } else {
          declareTypeInline(type, paramSyntax, description, all, required);
        }
      }
    }

    Optional<DslElementSyntax> typeDsl = builder.getDslResolver().resolve(type);
    if (paramDsl.allowsReferences() && typeDsl.isPresent() && typeDsl.get().supportsTopLevelDeclaration()
        && !builder.isImported(type)) {
      // We need to register the type, just in case people want to use it as global elements
      registerPojoType(type, description);
    }
  }

  private void declareTypeInline(ObjectType objectType, DslElementSyntax paramDsl, String description,
                                 List<TopLevelElement> all, boolean required) {
    registerPojoComplexType(objectType, null, description);
    String typeName = getBaseTypeName(objectType);
    QName localQName = new QName(paramDsl.getNamespace(), typeName, paramDsl.getPrefix());
    addChildElementTypeExtension(localQName, description, paramDsl.getElementName(),
                                 !paramDsl.supportsAttributeDeclaration() && required,
                                 all);
  }

  private void declareRefToType(ObjectType objectType, DslElementSyntax paramDsl, String description,
                                List<TopLevelElement> all, boolean required) {
    registerPojoSubtypes(objectType, builder.getTypesMapping().getSubTypes(objectType));
    addAbstractTypeRef(paramDsl, description, objectType, all, required);
  }

  /**
   * Adds a new {@link TopLevelElement element} to the {@link ExplicitGroup group} {@code all}
   */
  private void addChildElementTypeExtension(QName base, String description, String name, boolean required,
                                            List<TopLevelElement> all) {
    TopLevelElement objectElement = builder.createTopLevelElement(name, required ? ONE : ZERO, MAX_ONE);
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createTypeExtension(base));

    all.add(objectElement);
  }

  private void addImportedTypeElement(DslElementSyntax paramDsl, String description, MetadataType metadataType,
                                      List<TopLevelElement> all, boolean required) {

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(metadataType)
        .orElseThrow(() -> new IllegalArgumentException(format("The given type [%s] is not eligible for Import",
                                                               getId(metadataType))));

    if (paramDsl.isWrapped()) {

      TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), ZERO, MAX_ONE);
      objectElement.setComplexType(new LocalComplexType());
      objectElement.setAnnotation(builder.createDocAnnotation(description));

      if (typeDsl.isWrapped()) {
        objectElement.getComplexType()
            .setChoice(builder.createTypeRefChoiceLocalOrGlobal(typeDsl, metadataType, ZERO, UNBOUNDED));

      } else {
        ExplicitGroup sequence = new ExplicitGroup();
        sequence.setMinOccurs(ONE);
        sequence.setMaxOccurs(MAX_ONE);

        QName refQName = new QName(paramDsl.getNamespace(), getAbstractElementName(typeDsl), paramDsl.getPrefix());
        sequence.getParticle().add(objectFactory.createElement(builder.createRefElement(refQName, false)));
        objectElement.getComplexType().setSequence(sequence);
      }

      all.add(objectElement);

    } else {
      QName extensionBase = new QName(typeDsl.getNamespace(), sanitizeName(getId(metadataType)), typeDsl.getPrefix());
      addChildElementTypeExtension(extensionBase, description, paramDsl.getElementName(),
                                   !paramDsl.supportsAttributeDeclaration() && required, all);
    }
  }

  private void addAbstractTypeRef(DslElementSyntax paramDsl, String description, MetadataType metadataType,
                                  List<TopLevelElement> all, boolean required) {
    BigInteger minOccurs = !paramDsl.supportsAttributeDeclaration() && required ? ONE : ZERO;
    TopLevelElement objectElement = builder.createTopLevelElement(paramDsl.getElementName(), minOccurs, MAX_ONE);
    objectElement.setAnnotation(builder.createDocAnnotation(description));
    objectElement.setComplexType(createComplexTypeWithAbstractElementRef(metadataType));

    all.add(objectElement);
  }


  private LocalComplexType createComplexTypeWithAbstractElementRef(MetadataType type) {

    DslElementSyntax typeDsl = builder.getDslResolver().resolve(type)
        .orElseThrow(() -> new IllegalArgumentException(format("No element ref can be created for the given type [%s]",
                                                               getId(type))));

    LocalComplexType complexType = new LocalComplexType();
    if (typeDsl.isWrapped()) {
      complexType.setChoice(builder.createTypeRefChoiceLocalOrGlobal(typeDsl, type, ONE, MAX_ONE));
    } else {
      ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ONE);
      sequence.setMaxOccurs(MAX_ONE);

      sequence.getParticle().add(objectFactory.createElement(createRefToLocalElement(typeDsl, type)));
      complexType.setSequence(sequence);
    }

    return complexType;
  }

  private TopLevelElement createRefToLocalElement(DslElementSyntax typeDsl, MetadataType metadataType) {
    registerPojoType(metadataType, EMPTY);

    QName qName = new QName(typeDsl.getNamespace(), getAbstractElementName(typeDsl), typeDsl.getPrefix());
    return builder.createRefElement(qName, false);
  }

  String registerPojoType(MetadataType metadataType, String description) {
    return registerPojoType(metadataType, null, description);
  }

  /**
   * The given {@code type} type will be registered as a {@link TopLevelComplexType} in the current namespace if it was not
   * imported.
   * <p/>
   * If an abstract or concrete {@link TopLevelElement} declaration are required for this type, then they will also be registered.
   * This method is idempotent for any given {@code type}
   *
   * @param type a {@link MetadataType} describing a pojo type
   * @param baseType a {@link MetadataType} describing a pojo's base type
   * @param description the type's description
   * @return the reference name of the complexType
   */
  private String registerPojoType(MetadataType type, MetadataType baseType, String description) {
    if (!builder.isImported(type)) {
      registerPojoComplexType((ObjectType) type, (ObjectType) baseType, description);

      Optional<DslElementSyntax> typeDsl = builder.getDslResolver().resolve(type);
      if (typeDsl.isPresent() && shouldRegisterTypeAsElement(type, typeDsl.get())) {
        registerPojoGlobalElements(typeDsl.get(), (ObjectType) type, (ObjectType) baseType, description);
      }
    }

    return getBaseTypeName(type);
  }

  /**
   * @return whether or not the {@code type} requires the declaration of an abstract or concrete {@link TopLevelElement}
   */
  private boolean shouldRegisterTypeAsElement(MetadataType type, DslElementSyntax typeDsl) {
    return typeDsl.supportsTopLevelDeclaration() || typeDsl.isWrapped() || getSubstitutionGroup(type).isPresent() ||
        (type instanceof ObjectType && !builder.getTypesMapping().getSuperTypes((ObjectType) type).isEmpty());
  }

  /**
   * Registers the {@link TopLevelComplexType} associated to the given {@link ObjectType} in the current namespace
   *
   * @param type the {@link ObjectType} that will be represented by the registered {@link ComplexType}
   * @param baseType the {@code base} for the {@link ComplexType} {@code extension} declaration
   * @param description
   * @return a new {@link ComplexType} declaration for the given {@link ObjectType}
   */
  private ComplexType registerPojoComplexType(ObjectType type, ObjectType baseType, String description) {
    String typeId = getId(type);
    if (registeredComplexTypesHolders.get(typeId) != null) {
      return registeredComplexTypesHolders.get(typeId).getComplexType();
    }

    QName base = getComplexTypeBase(type, baseType);
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


  /**
   * @return the {@link QName} of the {@code base} type for which the new {@link ComplexType} declares an {@code extension}
   */
  private QName getComplexTypeBase(ObjectType type, ObjectType baseType) {
    Optional<QName> customBaseQName = getBaseType(type)
        .map(customDsl -> new QName(builder.getNamespaceUri(customDsl.getPrefix()), customDsl.getType(), customDsl.getPrefix()));

    if (customBaseQName.isPresent()) {
      //baseType was redefined by the user
      return customBaseQName.get();
    }

    Optional<DslElementSyntax> baseDsl = builder.getDslResolver().resolve(baseType);
    if (baseDsl.isPresent()) {
      return new QName(baseDsl.get().getNamespace(), getBaseTypeName(baseType), baseDsl.get().getPrefix());
    }

    return MULE_ABSTRACT_EXTENSION_TYPE;
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

    DslElementSyntax typeDsl = dsl.resolve(metadataType).get();
    List<TopLevelElement> childElements = new LinkedList<>();
    fields.forEach(field -> {

      String fieldName = field.getKey().getName().getLocalPart();
      DslElementSyntax fieldDsl = typeDsl.getContainedElement(fieldName).orElse(null);
      if (isFlattenedParameterGroup(field)) {
        declareGroupedFields(extension, childElements, field);
      } else {
        declareObjectField(fieldDsl, field, extension, childElements);
      }
    });

    if (!childElements.isEmpty()) {
      final ExplicitGroup all = new ExplicitGroup();
      all.setMaxOccurs(MAX_ONE);

      boolean requiredChilds = childElements.stream().anyMatch(builder::isRequired);
      all.setMinOccurs(requiredChilds ? ONE : ZERO);

      childElements.forEach(p -> all.getParticle().add(objectFactory.createElement(p)));
      extension.setSequence(all);
    }
    return complexType;
  }

  private void declareGroupedFields(ExtensionType extension, List<TopLevelElement> childElements, ObjectFieldType field) {
    DslElementSyntax groupDsl = dsl.resolve(field.getValue()).get();
    ((ObjectType) field.getValue()).getFields().forEach(
                                                        subField -> {
                                                          DslElementSyntax subFieldDsl = groupDsl
                                                              .getContainedElement(subField.getKey().getName().getLocalPart())
                                                              .orElse(null);
                                                          declareObjectField(subFieldDsl, subField, extension, childElements);
                                                        });
  }

  private void declareObjectField(DslElementSyntax fieldDsl, ObjectFieldType field, ExtensionType extension,
                                  List<TopLevelElement> all) {
    ParameterModel parameter = asParameter(field);
    if (fieldDsl == null) {
      fieldDsl = dsl.resolve(parameter);
    }

    final String id = getId(field.getValue());
    if (id.equals(TlsContextFactory.class.getName())) {
      builder.addTlsSupport(extension, all);
      return;
    }

    if (id.equals(Scheduler.class.getName())) {
      builder.addSchedulerSupport(all);
      return;
    }

    builder.declareAsParameter(field.getValue(), extension, parameter, fieldDsl, all);
  }

  private void registerPojoGlobalElements(DslElementSyntax typeDsl, ObjectType type, ObjectType baseType, String description) {

    if (registeredGlobalElementTypes.containsKey(globalTypeKey(typeDsl))) {
      return;
    }

    QName typeQName = getTypeQName(typeDsl, type);
    TopLevelElement abstractElement = registerAbstractElement(type, typeQName, typeDsl, baseType);
    if (typeDsl.supportsTopLevelDeclaration() || (typeDsl.supportsChildDeclaration() && typeDsl.isWrapped())
        || getSubstitutionGroup(type).isPresent() ||
        !builder.getTypesMapping().getSuperTypes(type).isEmpty()) {
      registerConcreteGlobalElement(typeDsl, description, abstractElement.getName(), typeQName);
    }
  }

  QName getTypeQName(DslElementSyntax typeDsl, MetadataType type) {
    return new QName(builder.getSchema().getTargetNamespace(), getBaseTypeName(type), typeDsl.getPrefix());
  }

  TopLevelElement registerAbstractElement(MetadataType type, DslElementSyntax typeDsl) {
    QName typeQName = getTypeQName(typeDsl, type);
    TopLevelElement abstractElement = registerAbstractElement(type, typeQName, typeDsl, null);

    return abstractElement;
  }

  private TopLevelElement registerAbstractElement(MetadataType type, QName typeQName, DslElementSyntax typeDsl,
                                                  ObjectType baseType) {
    TopLevelElement element = registeredGlobalElementTypes.get(typeDsl.getPrefix() + getAbstractElementName(typeDsl));
    if (element != null) {
      return element;
    }

    Optional<DslElementSyntax> baseDsl = builder.getDslResolver().resolve(baseType);
    if (typeDsl.isWrapped()) {
      createGlobalMuleExtensionAbstractElement(typeQName, typeDsl, baseDsl);
    }

    TopLevelElement abstractElement = new TopLevelElement();
    abstractElement.setName(getAbstractElementName(typeDsl));
    abstractElement.setAbstract(true);
    if (!typeDsl.supportsTopLevelDeclaration()) {
      abstractElement.setType(typeQName);
    }

    if (baseDsl.isPresent() || typeDsl.supportsTopLevelDeclaration()) {
      QName substitutionGroup = getAbstractElementSubstitutionGroup(typeDsl, baseDsl);
      abstractElement.setSubstitutionGroup(substitutionGroup);
    }
    //If user defined, substitutionGroup will be overridden
    getSubstitutionGroup(type)
        .ifPresent((substitutionGroup) -> abstractElement
            .setSubstitutionGroup(builder.resolveSubstitutionGroup(substitutionGroup)));

    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(abstractElement);
    registeredGlobalElementTypes.put(typeDsl.getPrefix() + getAbstractElementName(typeDsl), abstractElement);

    return abstractElement;
  }

  private QName getAbstractElementSubstitutionGroup(DslElementSyntax typeDsl, Optional<DslElementSyntax> baseDsl) {
    QName substitutionGroup;
    if (baseDsl.isPresent()) {
      DslElementSyntax base = baseDsl.get();
      String abstractElementName = typeDsl.supportsTopLevelDeclaration() ? getGlobalAbstractName(base)
          : getAbstractElementName(base);

      substitutionGroup = new QName(base.getNamespace(), abstractElementName, base.getPrefix());

    } else {
      if (typeDsl.isWrapped()) {
        substitutionGroup = new QName(typeDsl.getNamespace(), getGlobalAbstractName(typeDsl), typeDsl.getPrefix());
      } else {
        substitutionGroup = MULE_ABSTRACT_SHARED_EXTENSION;
      }
    }
    return substitutionGroup;
  }

  private void createGlobalMuleExtensionAbstractElement(QName typeQName, DslElementSyntax typeDsl,
                                                        Optional<DslElementSyntax> baseDsl) {
    QName globalSubGroup;
    if (baseDsl.isPresent()) {
      DslElementSyntax base = baseDsl.get();
      globalSubGroup = new QName(base.getNamespace(), getGlobalAbstractName(base), base.getPrefix());
    } else {
      globalSubGroup = MULE_ABSTRACT_SHARED_EXTENSION;
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

  void registerConcreteGlobalElement(DslElementSyntax typeDsl, String description,
                                     String abstractElementName, QName typeQName) {

    if (registeredGlobalElementTypes.containsKey(globalTypeKey(typeDsl))) {
      return;
    }

    TopLevelElement objectElement = new TopLevelElement();
    objectElement.setName(typeDsl.getElementName());

    objectElement.setSubstitutionGroup(new QName(typeDsl.getNamespace(), abstractElementName, typeDsl.getPrefix()));
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
    return typeDsl.getPrefix() + typeDsl.getElementName();
  }

  private ImmutableParameterModel asParameter(ObjectFieldType field) {
    return new ImmutableParameterModel(field.getKey().getName().getLocalPart(),
                                       "",
                                       field.getValue(),
                                       false,
                                       field.isRequired(),
                                       false,
                                       false,
                                       getExpressionSupport(field), getDefaultValue(field).orElse(null),
                                       BEHAVIOUR, ParameterDslConfiguration.getDefaultInstance(),
                                       null, getLayoutModel(field).orElse(null), null, emptyList(), emptySet(), null);
  }


  void registerPojoSubtypes(SubTypesModel subTypesModel) {
    registerPojoSubtypes(subTypesModel.getBaseType(), subTypesModel.getSubTypes());
  }

  void registerPojoSubtypes(MetadataType baseType, Collection<ObjectType> subTypes) {
    if (!builder.isImported(baseType)) {
      registerPojoType(baseType, EMPTY);
    }

    subTypes.forEach(subtype -> registerPojoType(subtype, baseType, EMPTY));
  }

  private String getId(MetadataType type) {
    return ExtensionMetadataTypeUtils.getId(type)
        .orElseThrow(() -> new IllegalArgumentException("Cannot register a type without id"));
  }

  LocalComplexType createTypeExtension(QName base) {
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

  static String getGlobalAbstractName(DslElementSyntax dsl) {
    return "global-" + getAbstractElementName(dsl);
  }

  static String getAbstractElementName(DslElementSyntax dsl) {
    return "abstract-" + dsl.getElementName();
  }
}
