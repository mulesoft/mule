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
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.extension.xml.dsl.api.XmlModelUtils.createXmlModelProperty;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_THREADING_PROFILE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_OPERATION_TRANSACTIONAL_ACTION_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.OPERATION_SUBSTITUTION_GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.STRING;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.TLS_CONTEXT_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.XML_NAMESPACE;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.connectivity.OperationTransactionalAction;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Annotation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Documentation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.FormChoice;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Group;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.GroupRef;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Import;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NoFixedFacet;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Restriction;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaTypeConversion;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Union;
import org.mule.runtime.module.extension.internal.model.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.xml.SchemaConstants;

import com.google.common.collect.ImmutableMap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Builder class to generate a XSD schema that describes a {@link ExtensionModel}
 *
 * @since 3.7.0
 */
public final class SchemaBuilder {

  private static final String UNBOUNDED = "unbounded";
  private static final String ABSTRACT_ELEMENT_MASK = "abstract-%s";

  private final Set<StringType> registeredEnums = new LinkedHashSet<>();
  private final Map<String, NamedGroup> substitutionGroups = new LinkedHashMap<>();
  private final ObjectFactory objectFactory = new ObjectFactory();
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final ConfigurationSchemaDelegate configurationSchemaDelegate = new ConfigurationSchemaDelegate(this);
  private final ConnectionProviderSchemaDelegate connectionProviderSchemaDelegate = new ConnectionProviderSchemaDelegate(this);
  private final OperationSchemaDelegate operationSchemaDelegate = new OperationSchemaDelegate(this);
  private final SourceSchemaDelegate sourceSchemaDelegate = new SourceSchemaDelegate(this);
  private final CollectionSchemaDelegate collectionDelegate = new CollectionSchemaDelegate(this);
  private final ObjectTypeSchemaDelegate objectTypeDelegate = new ObjectTypeSchemaDelegate(this);
  private final MapSchemaDelegate mapDelegate = new MapSchemaDelegate(this);

  private Schema schema;
  private boolean requiresTls = false;

  private DslSyntaxResolver dslResolver;
  private SubTypesMappingContainer subTypesMapping;
  private Map<MetadataType, MetadataType> importedTypes;

  public static SchemaBuilder newSchema(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty) {
    SchemaBuilder builder = new SchemaBuilder();
    builder.schema = new Schema();
    builder.schema.setTargetNamespace(xmlModelProperty.getNamespaceUri());
    builder.schema.setElementFormDefault(FormChoice.QUALIFIED);
    builder.schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);
    builder.withDslSyntaxResolver(new DslSyntaxResolver(extensionModel)).importXmlNamespace().importSpringFrameworkNamespace()
        .importMuleNamespace().importMuleExtensionNamespace();

    Optional<Map<MetadataType, MetadataType>> importedTypes =
        extensionModel.getModelProperty(ImportedTypesModelProperty.class).map(ImportedTypesModelProperty::getImportedTypes);
    builder.withImportedTypes(importedTypes.orElse(ImmutableMap.of()));

    Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
    builder.withTypeMapping(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : ImmutableMap.of());

    extensionModel.getModelProperty(ExportModelProperty.class).ifPresent(e -> builder.withExportedTypes(e.getExportedTypes()));

    return builder;
  }

  private SchemaBuilder withDslSyntaxResolver(DslSyntaxResolver dslSyntaxResolver) {
    this.dslResolver = dslSyntaxResolver;
    return this;
  }

  private SchemaBuilder withTypeMapping(Map<MetadataType, List<MetadataType>> subTypesMapping) {
    this.subTypesMapping = new SubTypesMappingContainer(subTypesMapping);
    subTypesMapping.forEach(objectTypeDelegate::registerPojoSubtypes);

    return this;
  }

  private SchemaBuilder withImportedTypes(Map<MetadataType, MetadataType> importedTypes) {
    this.importedTypes = importedTypes;
    importedTypes.values().forEach(ext -> registerExtensionImport(getType(ext)));
    return this;
  }

  private SchemaBuilder withExportedTypes(List<MetadataType> exportedTypes) {
    exportedTypes.stream().filter(t -> dslResolver.resolve(t).supportsTopLevelDeclaration())
        .forEach(t -> objectTypeDelegate.registerPojoType(t, t.getDescription().orElse(EMPTY)));
    return this;
  }

  SubTypesMappingContainer getSubTypesMapping() {
    return subTypesMapping;
  }

  Map<MetadataType, MetadataType> getImportedTypes() {
    return importedTypes;
  }

  DslSyntaxResolver getDslResolver() {
    return dslResolver;
  }

  public Schema build() {
    return schema;
  }

  private SchemaBuilder importXmlNamespace() {
    Import xmlImport = new Import();
    xmlImport.setNamespace(XML_NAMESPACE);
    schema.getIncludeOrImportOrRedefine().add(xmlImport);
    return this;
  }

  private SchemaBuilder importSpringFrameworkNamespace() {
    Import springFrameworkImport = new Import();
    springFrameworkImport.setNamespace(SPRING_FRAMEWORK_NAMESPACE);
    springFrameworkImport.setSchemaLocation(SPRING_FRAMEWORK_SCHEMA_LOCATION);
    schema.getIncludeOrImportOrRedefine().add(springFrameworkImport);
    return this;
  }

  private SchemaBuilder importMuleNamespace() {
    Import muleSchemaImport = new Import();
    muleSchemaImport.setNamespace(MULE_NAMESPACE);
    muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);
    schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    return this;
  }

  private SchemaBuilder importMuleExtensionNamespace() {
    Import muleExtensionImport = new Import();
    muleExtensionImport.setNamespace(MULE_EXTENSION_NAMESPACE);
    muleExtensionImport.setSchemaLocation(MULE_EXTENSION_SCHEMA_LOCATION);
    schema.getIncludeOrImportOrRedefine().add(muleExtensionImport);

    return this;
  }

  private SchemaBuilder importTlsNamespace() {
    Import tlsImport = new Import();
    tlsImport.setNamespace(MULE_TLS_NAMESPACE);
    tlsImport.setSchemaLocation(MULE_TLS_SCHEMA_LOCATION);
    schema.getIncludeOrImportOrRedefine().add(tlsImport);

    return this;
  }

  void addRetryPolicy(ExplicitGroup sequence) {
    TopLevelElement providerElementRetry = createRefElement(MULE_ABSTRACT_RECONNECTION_STRATEGY, false);
    sequence.getParticle().add(objectFactory.createElement(providerElementRetry));
  }

  public SchemaBuilder registerConnectionProviderElement(ConnectionProviderModel providerModel) {
    connectionProviderSchemaDelegate.registerConnectionProviderElement(providerModel);
    return this;
  }

  public SchemaBuilder registerConfigElement(final RuntimeConfigurationModel configurationModel) {
    configurationSchemaDelegate.registerConfigElement(schema, configurationModel);
    return this;
  }

  Attribute createNameAttribute(boolean required) {
    return createAttribute(SchemaConstants.ATTRIBUTE_NAME_NAME, load(String.class), required, NOT_SUPPORTED);
  }

  public SchemaBuilder registerOperation(OperationModel operationModel) {
    operationSchemaDelegate.registerOperation(operationModel);
    return this;
  }

  public SchemaBuilder registerMessageSource(SourceModel sourceModel) {

    sourceSchemaDelegate.registerMessageSource(sourceModel);
    return this;
  }

  void registerParameters(ExtensionType type, ExplicitGroup choice, Collection<ParameterModel> parameterModels) {
    for (final ParameterModel parameterModel : getSortedParameterModels(parameterModels)) {
      parameterModel.getType().accept(getParameterDeclarationVisitor(type, choice, parameterModel));
    }

    if (!choice.getParticle().isEmpty()) {
      type.setSequence(choice);
    }
  }

  /**
   * Sorts the given {@code parameterModels} so that infrastructure ones are firsts and the rest maintain their relative order. If
   * more than one infrastructure parameter is found, the subgroup is sorted alphabetically.
   * <p>
   * The {@link InfrastructureParameterModelProperty} is used to identify those parameters which are infrastructure
   *
   * @param parameterModels a {@link Collection} of {@link ParameterModel parameter models}
   * @return a sorted {@link List}
   */
  private List<ParameterModel> getSortedParameterModels(Collection<ParameterModel> parameterModels) {
    List<ParameterModel> sortedParameters = new ArrayList<>(parameterModels);
    sortedParameters.sort((left, right) -> {
      boolean isLeftInfrastructure = left.getModelProperty(InfrastructureParameterModelProperty.class).isPresent();
      boolean isRightInfrastructure = right.getModelProperty(InfrastructureParameterModelProperty.class).isPresent();

      if (!isLeftInfrastructure && !isRightInfrastructure) {
        return 0;
      }

      if (!isLeftInfrastructure) {
        return 1;
      }

      if (!isRightInfrastructure) {
        return -1;
      }

      return left.getName().compareTo(right.getName());
    });
    return sortedParameters;
  }

  public SchemaBuilder registerEnums() {
    registeredEnums.forEach(enumType -> registerEnum(schema, enumType));
    return this;
  }

  private void registerEnum(Schema schema, StringType enumType) {
    TopLevelSimpleType enumSimpleType = new TopLevelSimpleType();
    enumSimpleType.setName(sanitizeName(getId(enumType)) + SchemaConstants.ENUM_TYPE_SUFFIX);

    Union union = new Union();
    union.getSimpleType().add(createEnumSimpleType(enumType));
    union.getSimpleType().add(createExpressionAndPropertyPlaceHolderSimpleType());
    enumSimpleType.setUnion(union);

    schema.getSimpleTypeOrComplexTypeOrGroup().add(enumSimpleType);
  }

  private LocalSimpleType createExpressionAndPropertyPlaceHolderSimpleType() {
    LocalSimpleType expression = new LocalSimpleType();
    Restriction restriction = new Restriction();
    expression.setRestriction(restriction);
    restriction.setBase(SchemaConstants.MULE_PROPERTY_PLACEHOLDER_TYPE);

    return expression;
  }

  private LocalSimpleType createEnumSimpleType(MetadataType enumType) {
    LocalSimpleType enumValues = new LocalSimpleType();
    Restriction restriction = new Restriction();
    enumValues.setRestriction(restriction);
    restriction.setBase(STRING);


    EnumAnnotation<String> enumAnnotation = enumType.getAnnotation(EnumAnnotation.class)
        .orElseThrow(() -> new IllegalArgumentException("Cannot obtain enum values for the given type"));

    for (String value : enumAnnotation.getValues()) {
      NoFixedFacet noFixedFacet = objectFactory.createNoFixedFacet();
      noFixedFacet.setValue(value);

      JAXBElement<NoFixedFacet> enumeration = objectFactory.createEnumeration(noFixedFacet);
      enumValues.getRestriction().getFacets().add(enumeration);
    }

    return enumValues;
  }

  String getTopLevelAbstractName(DslElementSyntax typeDsl) {
    return format(ABSTRACT_ELEMENT_MASK, typeDsl.getElementName());
  }

  Attribute createAttribute(String name, MetadataType type, boolean required, ExpressionSupport expressionSupport) {
    return createAttribute(name, EMPTY, type, null, required, expressionSupport);
  }

  private Attribute createAttribute(final String name, String description, final MetadataType type, Object defaultValue,
                                    boolean required, final ExpressionSupport expressionSupport) {
    final Attribute attribute = new Attribute();
    attribute.setUse(required ? SchemaConstants.USE_REQUIRED : SchemaConstants.USE_OPTIONAL);
    attribute.setAnnotation(createDocAnnotation(description));

    if (defaultValue instanceof String && StringUtils.isNotBlank(defaultValue.toString())) {
      attribute.setDefault(defaultValue.toString());
    }

    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitString(StringType stringType) {
        Optional<EnumAnnotation> enumAnnotation = stringType.getAnnotation(EnumAnnotation.class);

        if (enumAnnotation.isPresent()) {
          visitEnum(stringType);
        } else {
          defaultVisit(stringType);
        }
      }

      private void visitEnum(StringType enumType) {
        attribute.setName(name);

        String typeName = getId(enumType);
        if (OperationTransactionalAction.class.getName().equals(typeName)) {
          attribute.setType(MULE_EXTENSION_OPERATION_TRANSACTIONAL_ACTION_TYPE);
        } else {
          attribute.setType(new QName(schema.getTargetNamespace(), sanitizeName(typeName) + SchemaConstants.ENUM_TYPE_SUFFIX));
          registeredEnums.add(enumType);
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        attribute.setName(name);
        attribute.setType(SchemaTypeConversion.convertType(type, expressionSupport));
      }
    });

    return attribute;
  }

  Attribute createValueAttribute(MetadataType genericType) {
    return createAttribute(ATTRIBUTE_NAME_VALUE, genericType, true, SUPPORTED);
  }

  TopLevelElement createTypeRef(ObjectType type, boolean isRequired) {
    if (importedTypes.get(type) == null) {
      objectTypeDelegate.registerPojoType(type, EMPTY);
    }

    DslElementSyntax refDsl = dslResolver.resolve(type);
    QName refQName = new QName(refDsl.getNamespaceUri(), getTopLevelAbstractName(refDsl), refDsl.getNamespace());
    return createRefElement(refQName, isRequired);
  }

  QName getSubstitutionGroup(Class<?> type) {
    return new QName(schema.getTargetNamespace(), registerExtensibleElement(type));
  }

  private String registerExtensibleElement(Class<?> type) {
    Extensible extensible = type.getAnnotation(Extensible.class);
    checkArgument(extensible != null, format("Type %s is not extensible", type.getName()));

    String name = extensible.alias();
    if (StringUtils.isBlank(name)) {
      name = type.getName() + OPERATION_SUBSTITUTION_GROUP_SUFFIX;
    }

    NamedGroup group = substitutionGroups.get(name);
    if (group == null) {
      // register abstract element to serve as substitution
      TopLevelElement element = new TopLevelElement();
      element.setName(name);
      element.setAbstract(true);
      element.setSubstitutionGroup(MULE_ABSTRACT_MESSAGE_PROCESSOR);
      schema.getSimpleTypeOrComplexTypeOrGroup().add(element);

      group = new NamedGroup();
      group.setName(getGroupName(name));
      schema.getSimpleTypeOrComplexTypeOrGroup().add(group);

      substitutionGroups.put(name, group);

      element = new TopLevelElement();
      element.setRef(new QName(schema.getTargetNamespace(), name));
      group.getChoice().getParticle().add(objectFactory.createElement(element));
    }

    return name;
  }

  private String getGroupName(String name) {
    return name + GROUP_SUFFIX;
  }

  ExtensionType registerExecutableType(String name, ParameterizedModel parameterizedModel, QName base) {
    TopLevelComplexType complexType = new TopLevelComplexType();
    complexType.setName(name);

    ComplexContent complexContent = new ComplexContent();
    complexType.setComplexContent(complexContent);
    final ExtensionType complexContentExtension = new ExtensionType();
    complexContentExtension.setBase(base);
    complexContent.setExtension(complexContentExtension);

    Attribute configAttr = createAttribute(CONFIG_ATTRIBUTE, CONFIG_ATTRIBUTE_DESCRIPTION, true, SUBSTITUTABLE_NAME);
    complexContentExtension.getAttributeOrAttributeGroup().add(configAttr);

    final ExplicitGroup all = new ExplicitGroup();
    complexContentExtension.setSequence(all);

    for (final ParameterModel parameterModel : parameterizedModel.getParameterModels()) {
      MetadataType parameterType = parameterModel.getType();

      if (isOperation(parameterType)) {
        String maxOccurs = parameterType instanceof ArrayType ? UNBOUNDED : "1";
        generateNestedProcessorElement(all, parameterModel, maxOccurs);
      } else {
        parameterType.accept(getParameterDeclarationVisitor(complexContentExtension, all, parameterModel));
      }
    }

    if (all.getParticle().isEmpty()) {
      complexContentExtension.setSequence(null);
    }

    schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

    return complexContentExtension;
  }

  MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all,
                                                     final ParameterModel parameterModel) {
    final DslElementSyntax paramDsl = dslResolver.resolve(parameterModel);
    return getParameterDeclarationVisitor(extensionType, all, parameterModel.getName(), parameterModel.getDescription(),
                                          parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                                          parameterModel.getDefaultValue(), paramDsl);
  }

  private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all,
                                                             final String name, final String description,
                                                             ExpressionSupport expressionSupport, boolean required,
                                                             Object defaultValue, DslElementSyntax paramDsl) {
    return new MetadataTypeVisitor() {

      private boolean forceOptional = paramDsl.supportsChildDeclaration() || !required;

      @Override
      public void visitArrayType(ArrayType arrayType) {
        defaultVisit(arrayType);
        if (paramDsl.supportsChildDeclaration()) {
          collectionDelegate.generateCollectionElement(arrayType, paramDsl, description, false, all);
        }
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        defaultVisit(dictionaryType);
        if (paramDsl.supportsChildDeclaration()) {
          mapDelegate.generateMapElement(dictionaryType, paramDsl, description, false, all);

        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (getId(objectType).equals(TlsContextFactory.class.getName())) {
          addTlsSupport(extensionType, all);
          return;
        }

        if (getId(objectType).equals(ThreadingProfile.class.getName())) {
          addAttributeAndElement(extensionType, all, THREADING_PROFILE_ATTRIBUTE_NAME, MULE_ABSTRACT_THREADING_PROFILE);
          return;
        }

        defaultVisit(objectType);

        objectTypeDelegate.generatePojoElement(objectType, paramDsl, name, description, all);
      }

      @Override
      public void visitString(StringType stringType) {
        if (paramDsl.supportsChildDeclaration()) {
          generateTextElement(paramDsl, description, isRequired(forceOptional, required), all);
        } else {
          defaultVisit(stringType);
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        extensionType.getAttributeOrAttributeGroup().add(createAttribute(name, description, metadataType, defaultValue,
                                                                         isRequired(forceOptional, required), expressionSupport));
      }

      private boolean isRequired(boolean forceOptional, boolean required) {
        return !forceOptional && required;
      }
    };
  }

  private XmlModelProperty registerExtensionImport(Class<?> extensionType) {
    XmlModelProperty importedExtensionXml =
        createXmlModelProperty(getAnnotation(extensionType, Xml.class), getAnnotation(extensionType, Extension.class).name(), "");

    Import schemaImport = new Import();
    schemaImport.setNamespace(importedExtensionXml.getNamespaceUri());
    schemaImport.setSchemaLocation(importedExtensionXml.getSchemaLocation());
    if (!schema.getIncludeOrImportOrRedefine().contains(schemaImport)) {
      schema.getIncludeOrImportOrRedefine().add(schemaImport);
    }
    return importedExtensionXml;
  }

  private boolean isOperation(MetadataType type) {
    ValueHolder<Boolean> isOperation = new ValueHolder<>(false);
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (NestedProcessor.class.isAssignableFrom(getType(objectType))) {
          isOperation.set(true);
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }
    });

    return isOperation.get();
  }

  private void addTlsSupport(ExtensionType extensionType, ExplicitGroup all) {
    if (!requiresTls) {
      importTlsNamespace();
      requiresTls = true;
    }

    addAttributeAndElement(extensionType, all, TLS_ATTRIBUTE_NAME, TLS_CONTEXT_TYPE);
  }

  private void addAttributeAndElement(ExtensionType extensionType, ExplicitGroup all, String attributeName, QName elementRef) {

    extensionType.getAttributeOrAttributeGroup()
        .add(createAttribute(attributeName, load(String.class), false, ExpressionSupport.NOT_SUPPORTED));

    all.getParticle().add(objectFactory.createElement(createRefElement(elementRef, false)));
  }

  TopLevelElement createRefElement(QName elementRef, boolean isRequired) {
    TopLevelElement element = new TopLevelElement();
    element.setRef(elementRef);
    element.setMinOccurs(isRequired ? ONE : ZERO);
    element.setMaxOccurs("1");
    return element;
  }

  private void generateNestedProcessorElement(ExplicitGroup all, ParameterModel parameterModel, String maxOccurs) {
    LocalComplexType collectionComplexType = new LocalComplexType();
    GroupRef group = generateNestedProcessorGroup(parameterModel, maxOccurs);
    collectionComplexType.setGroup(group);
    collectionComplexType.setAnnotation(createDocAnnotation(parameterModel.getDescription()));

    TopLevelElement collectionElement = new TopLevelElement();
    collectionElement.setName(hyphenize(parameterModel.getName()));
    collectionElement.setMinOccurs(parameterModel.isRequired() ? ONE : ZERO);
    collectionElement.setMaxOccurs(maxOccurs);
    collectionElement.setComplexType(collectionComplexType);
    collectionElement.setAnnotation(createDocAnnotation(EMPTY));
    all.getParticle().add(objectFactory.createElement(collectionElement));
  }

  private GroupRef generateNestedProcessorGroup(ParameterModel parameterModel, String maxOccurs) {
    QName ref = MULE_MESSAGE_PROCESSOR_TYPE;
    TypeRestrictionModelProperty restrictionCapability =
        parameterModel.getModelProperty(TypeRestrictionModelProperty.class).orElse(null);
    if (restrictionCapability != null) {
      ref = getSubstitutionGroup(restrictionCapability.getType());
      ref = new QName(ref.getNamespaceURI(), getGroupName(ref.getLocalPart()), ref.getPrefix());
    }

    GroupRef group = new GroupRef();
    group.setRef(ref);
    group.setMinOccurs(parameterModel.isRequired() ? ONE : ZERO);
    group.setMaxOccurs(maxOccurs);

    return group;
  }

  private Attribute createAttribute(String name, String description, boolean optional, QName type) {
    Attribute attr = new Attribute();
    attr.setName(name);
    attr.setUse(optional ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);
    attr.setType(type);

    if (description != null) {
      attr.setAnnotation(createDocAnnotation(description));
    }

    return attr;
  }

  Schema getSchema() {
    return schema;
  }

  Annotation createDocAnnotation(String content) {
    if (StringUtils.isBlank(content)) {
      return null;
    }

    Annotation annotation = new Annotation();
    Documentation doc = new Documentation();
    doc.getContent().add(content);
    annotation.getAppinfoOrDocumentation().add(doc);
    return annotation;
  }

  MetadataType load(Class<?> clazz) {
    return typeLoader.load(clazz);
  }

  TopLevelElement createTopLevelElement(String name, BigInteger minOccurs, String maxOccurs) {
    TopLevelElement element = new TopLevelElement();
    element.setName(name);
    element.setMinOccurs(minOccurs);
    element.setMaxOccurs(maxOccurs);
    return element;
  }

  TopLevelElement createTopLevelElement(String name, BigInteger minOccurs, String maxOccurs, LocalComplexType type) {
    TopLevelElement element = createTopLevelElement(name, minOccurs, maxOccurs);
    element.setComplexType(type);

    return element;
  }

  private void generateTextElement(DslElementSyntax paramDsl, String description, boolean isRequired, Group all) {
    TopLevelElement textElement = createTopLevelElement(paramDsl.getElementName(), isRequired ? ONE : ZERO, "1");
    textElement.setAnnotation(createDocAnnotation(description));
    textElement.setType(STRING);

    all.getParticle().add(objectFactory.createElement(textElement));
  }

}
