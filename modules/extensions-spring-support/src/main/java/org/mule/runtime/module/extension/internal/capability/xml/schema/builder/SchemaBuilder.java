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
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.builder.ObjectTypeSchemaDelegate.getAbstractElementName;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_THREADING_PROFILE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_OPERATION_TRANSACTIONAL_ACTION_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.STRING;
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
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ElementDslModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Annotation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Documentation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.FormChoice;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Group;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Import;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NoFixedFacet;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Restriction;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaTypeConversion;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Union;
import org.mule.runtime.module.extension.internal.model.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.xml.SchemaConstants;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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

  private static final String GLOBAL_ABSTRACT_ELEMENT_MASK = "global-%s";

  private final Set<StringType> registeredEnums = new LinkedHashSet<>();

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

  private ExtensionModel extensionModel;
  private DslSyntaxResolver dslResolver;
  private SubTypesMappingContainer subTypesMapping;
  private Set<ImportedTypeModel> importedTypes;
  private DslResolvingContext dslExtensionContext;

  public static SchemaBuilder newSchema(ExtensionModel extensionModel, XmlDslModel xmlDslModel,
                                        DslResolvingContext dslContext) {

    SchemaBuilder builder = new SchemaBuilder();
    builder.extensionModel = extensionModel;
    builder.schema = new Schema();
    builder.schema.setTargetNamespace(xmlDslModel.getNamespaceUri());
    builder.schema.setElementFormDefault(FormChoice.QUALIFIED);
    builder.schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);
    builder.withDslSyntaxResolver(extensionModel, dslContext)
        .importXmlNamespace()
        .importSpringFrameworkNamespace()
        .importMuleNamespace()
        .importMuleExtensionNamespace();

    builder.withImportedTypes(extensionModel.getImportedTypes());

    builder.withTypeMapping(extensionModel.getSubTypes());
    builder.withTypes(extensionModel.getTypes());

    return builder;
  }

  private SchemaBuilder withDslSyntaxResolver(ExtensionModel model, DslResolvingContext dslContext) {
    this.dslExtensionContext = dslContext;
    this.dslResolver = new DslSyntaxResolver(model, dslContext);
    return this;
  }

  private SchemaBuilder withTypeMapping(Collection<SubTypesModel> subTypesModels) {
    this.subTypesMapping = new SubTypesMappingContainer(subTypesModels);
    subTypesModels.forEach(objectTypeDelegate::registerPojoSubtypes);
    return this;
  }

  private SchemaBuilder withImportedTypes(Set<ImportedTypeModel> importedTypes) {
    this.importedTypes = importedTypes;
    importedTypes.forEach(type -> dslExtensionContext.getExtension(type.getOriginExtensionName())
        .ifPresent(this::registerExtensionImport));
    return this;
  }

  private SchemaBuilder withTypes(Collection<ObjectType> types) {
    types.stream().filter(t -> {
      Optional<DslElementSyntax> typeDsl = dslResolver.resolve(t);
      return typeDsl.isPresent() && typeDsl.get().supportsTopLevelDeclaration();
    }).forEach(t -> objectTypeDelegate.registerPojoType(t, t.getDescription().orElse(EMPTY)));

    return this;
  }

  SubTypesMappingContainer getSubTypesMapping() {
    return subTypesMapping;
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

  public SchemaBuilder registerConfigElement(ConfigurationModel configurationModel) {
    configurationSchemaDelegate.registerConfigElement(schema, configurationModel, dslResolver.resolve(configurationModel));
    return this;
  }

  Attribute createNameAttribute(boolean required) {
    return createAttribute(SchemaConstants.ATTRIBUTE_NAME_NAME, load(String.class), required, NOT_SUPPORTED);
  }

  public SchemaBuilder registerOperation(OperationModel operationModel) {
    operationSchemaDelegate.registerOperation(operationModel, dslResolver.resolve(operationModel));
    return this;
  }

  public SchemaBuilder registerMessageSource(SourceModel sourceModel) {

    sourceSchemaDelegate.registerMessageSource(sourceModel, dslResolver.resolve(sourceModel));
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


  /**
   * Creates a {@link ExplicitGroup Choice} group that supports {@code refs} to both the {@code global} and {@code local}
   * abstract elements for the given {@code type}.
   * This is required in order to allow subtypes that support top-level declaration along with other subtypes that
   * support only local declarations as childs.
   * <p/>
   * For example, a resulting choice group for a type of name {@code TypeName} will look like:
   *
   * <xs:complexType>
   *  <xs:choice minOccurs="1" maxOccurs="1">
   *    <xs:element minOccurs="0" maxOccurs="1" ref="ns:abstract-type-name"></xs:element>
   *    <xs:element minOccurs="0" maxOccurs="1" ref="ns:global-abstract-type-name"></xs:element>
   *  </xs:choice>
   * </xs:complexType>
   * <p/>
   *
   * @param typeDsl {@link DslElementSyntax} of the referenced type
   * @param type the {@link MetadataType type} of the base element that will be referenced
   * @param minOccurs {@link BigInteger#ZERO} if the {@code group} is optional or {@link BigInteger#ONE} if required
   * @param maxOccurs the maximum number of occurrences for this group
   * @return a {@link ExplicitGroup Choice} group with the necessary options for this case
   */
  ExplicitGroup createTypeRefChoiceLocalOrGlobal(DslElementSyntax typeDsl, MetadataType type, BigInteger minOccurs,
                                                 String maxOccurs) {
    if (!isImported(type)) {
      objectTypeDelegate.registerPojoType(type, EMPTY);
      objectTypeDelegate.registerAbstractElement(type, typeDsl);
      if (typeDsl.supportsTopLevelDeclaration() || (typeDsl.supportsChildDeclaration() && typeDsl.isWrapped())) {
        objectTypeDelegate.registerConcreteGlobalElement(typeDsl, EMPTY, getAbstractElementName(typeDsl),
                                                         objectTypeDelegate.getTypeQName(typeDsl, type));
      }
    }

    final ExplicitGroup choice = new ExplicitGroup();
    choice.setMinOccurs(minOccurs);
    choice.setMaxOccurs(maxOccurs);

    QName refAbstract = new QName(typeDsl.getNamespaceUri(), getAbstractElementName(typeDsl), typeDsl.getNamespace());
    TopLevelElement localAbstractElementRef = createRefElement(refAbstract, false);
    choice.getParticle().add(objectFactory.createElement(localAbstractElementRef));

    QName refGlobal = new QName(typeDsl.getNamespaceUri(), format(GLOBAL_ABSTRACT_ELEMENT_MASK, getAbstractElementName(typeDsl)),
                                typeDsl.getNamespace());
    TopLevelElement topLevelElementRef = createRefElement(refGlobal, false);
    choice.getParticle().add(objectFactory.createElement(topLevelElementRef));

    return choice;
  }

  boolean isImported(MetadataType type) {
    return importedTypes.stream()
        .filter(t -> Objects.equals(getTypeId(t.getImportedType()), getTypeId(type)))
        .findFirst()
        .isPresent();
  }

  private String getTypeId(MetadataType type) {
    return getId(type);
  }

  /**
   * Creates a {@code ref} to the abstract element of the given {@code type} based on its {@code typeDsl} declaration.
   * Anywhere this ref element is used, the schema will allow to declare an xml element with a substitution group that matches
   * the referenced abstract-element
   * <p/>
   * For example, if we create this ref element to the type of name {@code TypeName}:
   *
   * <xs:element minOccurs="0" maxOccurs="1" ref="ns:abstract-type-name"></xs:element>
   *
   * <p/>
   * Then, any of the following elements are allowed to be used where the ref exists:
   *
   * <xs:element type="ns:org.mule.test.SomeType" substitutionGroup="ns:abstract-type-name" name="some-type"></xs:element>
   *
   * <xs:element type="ns:org.mule.test.OtherType" substitutionGroup="ns:abstract-type-name" name="other-type"></xs:element>
   *
   * @param typeDsl {@link DslElementSyntax} of the referenced {@code type}
   * @param type {@link MetadataType} of the referenced {@code type}
   * @param isRequired whether or not the element element is required
   * @return the {@link TopLevelElement element} representing the reference
   */
  TopLevelElement createTypeRef(DslElementSyntax typeDsl, ObjectType type, boolean isRequired) {

    if (!isImported(type)) {
      objectTypeDelegate.registerPojoType(type, EMPTY);
      objectTypeDelegate.registerAbstractElement(type, typeDsl);
      objectTypeDelegate.registerConcreteGlobalElement(typeDsl, EMPTY, getAbstractElementName(typeDsl),
                                                       objectTypeDelegate.getTypeQName(typeDsl, type));
    }

    QName refQName = new QName(typeDsl.getNamespaceUri(), getAbstractElementName(typeDsl), typeDsl.getNamespace());
    return createRefElement(refQName, isRequired);
  }

  MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all,
                                                     final ParameterModel parameterModel) {
    final DslElementSyntax paramDsl = dslResolver.resolve(parameterModel);
    return getParameterDeclarationVisitor(extensionType, all, parameterModel.getName(), parameterModel.getDescription(),
                                          parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                                          parameterModel.getDefaultValue(), paramDsl,
                                          parameterModel.getDslModel());
  }

  private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all,
                                                             final String name, final String description,
                                                             ExpressionSupport expressionSupport, boolean required,
                                                             Object defaultValue, DslElementSyntax paramDsl,
                                                             ElementDslModel dslModel) {
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
        final String id = getId(objectType);
        if (id.equals(TlsContextFactory.class.getName())) {
          addTlsSupport(extensionType, all);
          return;
        }

        if (id.equals(ThreadingProfile.class.getName())) {
          addAttributeAndElement(extensionType, all, THREADING_PROFILE_ATTRIBUTE_NAME, MULE_ABSTRACT_THREADING_PROFILE);
          return;
        }

        defaultVisit(objectType);
        objectTypeDelegate.generatePojoElement(objectType, paramDsl, dslModel, description, all);
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

  private XmlDslModel registerExtensionImport(ExtensionModel extension) {
    XmlDslModel languageModel = extension.getXmlDslModel();
    Import schemaImport = new Import();
    schemaImport.setNamespace(languageModel.getNamespaceUri());
    schemaImport.setSchemaLocation(languageModel.getSchemaLocation());
    if (!schema.getIncludeOrImportOrRedefine().contains(schemaImport)) {
      schema.getIncludeOrImportOrRedefine().add(schemaImport);
    }

    return languageModel;
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

  Attribute createAttribute(String name, String description, boolean optional, QName type) {
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

  ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  private void generateTextElement(DslElementSyntax paramDsl, String description, boolean isRequired, Group all) {
    TopLevelElement textElement = createTopLevelElement(paramDsl.getElementName(), isRequired ? ONE : ZERO, "1");
    textElement.setAnnotation(createDocAnnotation(description));
    textElement.setType(STRING);

    all.getParticle().add(objectFactory.createElement(textElement));
  }
}
