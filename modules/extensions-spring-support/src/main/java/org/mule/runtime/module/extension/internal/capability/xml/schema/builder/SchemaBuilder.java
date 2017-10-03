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
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EE_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.ENUM_TYPE_SUFFIX;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_OPERATION_TRANSACTIONAL_ACTION_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_TRANSACTION_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.STRING;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.TLS_CONTEXT_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.USE_OPTIONAL;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.USE_REQUIRED;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.XML_NAMESPACE;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getSubstitutionGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.componentHasAnImplicitConfiguration;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.builder.ObjectTypeSchemaDelegate.getAbstractElementName;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.config.internal.dsl.SchemaConstants;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.SubstitutionGroup;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.runtime.extension.api.util.ParameterModelComparator;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Annotation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Documentation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.FormChoice;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Import;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NoFixedFacet;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Restriction;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaTypeConversion;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Union;

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
  private Schema schema;
  private boolean requiresTls = false;

  private ExtensionModel extensionModel;
  private DslSyntaxResolver dslResolver;
  private Set<ImportedTypeModel> importedTypes;

  private TypeCatalog typesMapping;
  private DslResolvingContext dslContext;
  private ConfigurationSchemaDelegate configurationSchemaDelegate;
  private ConnectionProviderSchemaDelegate connectionProviderSchemaDelegate;
  private OperationSchemaDelegate operationSchemaDelegate;
  private SourceSchemaDelegate sourceSchemaDelegate;
  private CollectionSchemaDelegate collectionDelegate;
  private ObjectTypeSchemaDelegate objectTypeDelegate;
  private MapSchemaDelegate mapDelegate;

  public static SchemaBuilder newSchema(ExtensionModel extensionModel, XmlDslModel xmlDslModel, DslResolvingContext dslContext) {
    SchemaBuilder builder = new SchemaBuilder();
    builder.extensionModel = extensionModel;
    builder.schema = new Schema();
    builder.schema.setTargetNamespace(xmlDslModel.getNamespace());
    builder.schema.setElementFormDefault(FormChoice.QUALIFIED);
    builder.schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);
    builder.withDslSyntaxResolver(extensionModel, dslContext)
        .importXmlNamespace()
        .importSpringFrameworkNamespace()
        .importMuleNamespace();

    builder.initialiseDelegates();

    builder.withImportedTypes(extensionModel.getImportedTypes());
    builder.withTypeMapping(extensionModel);
    builder.withTypes(extensionModel.getTypes());

    return builder;
  }

  private void initialiseDelegates() {
    configurationSchemaDelegate = new ConfigurationSchemaDelegate(this);
    connectionProviderSchemaDelegate = new ConnectionProviderSchemaDelegate(this);
    operationSchemaDelegate = new OperationSchemaDelegate(this);
    sourceSchemaDelegate = new SourceSchemaDelegate(this);
    collectionDelegate = new CollectionSchemaDelegate(this);
    objectTypeDelegate = new ObjectTypeSchemaDelegate(this);
    mapDelegate = new MapSchemaDelegate(this, typeLoader);
  }

  private SchemaBuilder withDslSyntaxResolver(ExtensionModel model, DslResolvingContext dslContext) {
    this.dslContext = dslContext;
    this.dslResolver = DslSyntaxResolver.getDefault(model, dslContext);
    return this;
  }

  private SchemaBuilder withTypeMapping(ExtensionModel model) {
    this.typesMapping = TypeCatalog.getDefault(singleton(model));
    model.getSubTypes().forEach(objectTypeDelegate::registerPojoSubtypes);
    return this;
  }

  private SchemaBuilder withImportedTypes(Set<ImportedTypeModel> importedTypes) {
    this.importedTypes = importedTypes;
    importedTypes.forEach(type -> dslContext.getExtensionForType(getTypeId(type.getImportedType()))
        .ifPresent(this::registerExtensionImport));
    return this;
  }

  private SchemaBuilder withTypes(Collection<ObjectType> types) {
    types.stream().filter(t -> {
      Optional<DslElementSyntax> typeDsl = dslResolver.resolve(t);
      return (typeDsl.isPresent() && typeDsl.get().supportsTopLevelDeclaration())
          || getSubstitutionGroup(t).isPresent();
    }).forEach(t -> objectTypeDelegate.registerPojoType(t, t.getDescription().orElse(EMPTY)));

    return this;
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

  private Import createMuleImport() {
    Import muleSchemaImport = new Import();
    muleSchemaImport.setNamespace(CORE_NAMESPACE);
    muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);
    return muleSchemaImport;
  }

  private SchemaBuilder importMuleNamespace() {
    Import muleSchemaImport = createMuleImport();
    schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    return this;
  }

  private Import createMuleEEImport() {
    Import muleSchemaImport = new Import();
    muleSchemaImport.setNamespace(EE_NAMESPACE);
    muleSchemaImport.setSchemaLocation(EE_SCHEMA_LOCATION);
    return muleSchemaImport;
  }

  private void importIfNotImported(Import newImport) {
    if (!schema.getIncludeOrImportOrRedefine().contains(newImport)) {
      schema.getIncludeOrImportOrRedefine().add(newImport);
    }
  }

  private SchemaBuilder importTlsNamespace() {
    Import tlsImport = new Import();
    tlsImport.setNamespace(MULE_TLS_NAMESPACE);
    tlsImport.setSchemaLocation(MULE_TLS_SCHEMA_LOCATION);
    schema.getIncludeOrImportOrRedefine().add(tlsImport);

    return this;
  }

  public SchemaBuilder registerConnectionProviderElement(ConnectionProviderModel providerModel) {
    connectionProviderSchemaDelegate.registerConnectionProviderElement(providerModel, dslResolver.resolve(providerModel));
    return this;
  }

  public SchemaBuilder registerConfigElement(ConfigurationModel configurationModel) {
    configurationSchemaDelegate.registerConfigElement(schema, configurationModel, dslResolver.resolve(configurationModel));
    return this;
  }

  Attribute createNameAttribute(boolean required) {
    return createAttribute(NAME_ATTRIBUTE_NAME, load(String.class), required, NOT_SUPPORTED);
  }

  public SchemaBuilder registerOperation(ComponentModel operationModel) {
    operationSchemaDelegate.registerOperation(operationModel,
                                              dslResolver.resolve(operationModel),
                                              componentHasAnImplicitConfiguration(extensionModel, operationModel));
    return this;
  }

  public SchemaBuilder registerMessageSource(SourceModel sourceModel) {
    sourceSchemaDelegate.registerMessageSource(sourceModel,
                                               dslResolver.resolve(sourceModel),
                                               componentHasAnImplicitConfiguration(extensionModel, sourceModel));
    return this;
  }

  List<TopLevelElement> registerParameters(ExtensionType type, Collection<ParameterModel> parameterModels) {
    List<TopLevelElement> all = new ArrayList<>(parameterModels.size());
    getSortedParameterModels(parameterModels).stream()
        .filter(p -> !p.getModelProperty(QNameModelProperty.class).isPresent())
        .forEach(parameterModel -> {
          DslElementSyntax paramDsl = dslResolver.resolve(parameterModel);
          declareAsParameter(parameterModel.getType(), type, parameterModel, paramDsl, all);
        });

    return all;
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
    sortedParameters.sort(new ParameterModelComparator(true));
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
    attribute.setUse(required ? USE_REQUIRED : USE_OPTIONAL);
    attribute.setAnnotation(createDocAnnotation(description));

    if (defaultValue instanceof String && isNotBlank(defaultValue.toString())) {
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

        String typeName = getTypeId(enumType);
        if (OperationTransactionalAction.class.getName().equals(typeName)) {
          attribute.setType(MULE_OPERATION_TRANSACTIONAL_ACTION_TYPE);
        } else if (TransactionType.class.getName().equals(typeName)) {
          attribute.setType(MULE_TRANSACTION_TYPE);
          attribute.setDefault(LOCAL.name());
        } else {
          attribute.setType(new QName(schema.getTargetNamespace(), sanitizeName(typeName) + ENUM_TYPE_SUFFIX));
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
    return createAttribute(VALUE_ATTRIBUTE_NAME, genericType, true, SUPPORTED);
  }


  /**
   * Creates a {@link ExplicitGroup Choice} group that supports {@code refs} to both the {@code global} and {@code local} abstract
   * elements for the given {@code type}. This is required in order to allow subtypes that support top-level declaration along
   * with other subtypes that support only local declarations as childs.
   * <p/>
   * For example, a resulting choice group for a type of name {@code TypeName} will look like:
   * <p>
   * <xs:complexType> <xs:choice minOccurs="1" maxOccurs="1">
   * <xs:element minOccurs="0" maxOccurs="1" ref="ns:abstract-type-name"></xs:element>
   * <xs:element minOccurs="0" maxOccurs="1" ref="ns:global-abstract-type-name"></xs:element> </xs:choice> </xs:complexType>
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

    QName refAbstract = new QName(typeDsl.getNamespace(), getAbstractElementName(typeDsl), typeDsl.getPrefix());
    TopLevelElement localAbstractElementRef = createRefElement(refAbstract, true);
    choice.getParticle().add(objectFactory.createElement(localAbstractElementRef));

    QName refGlobal = new QName(typeDsl.getNamespace(), format(GLOBAL_ABSTRACT_ELEMENT_MASK, getAbstractElementName(typeDsl)),
                                typeDsl.getPrefix());
    TopLevelElement topLevelElementRef = createRefElement(refGlobal, true);
    choice.getParticle().add(objectFactory.createElement(topLevelElementRef));

    return choice;
  }

  boolean isImported(MetadataType type) {
    return importedTypes.stream()
        .anyMatch(t -> Objects.equals(getTypeId(t.getImportedType()), getTypeId(type)));
  }

  private String getTypeId(MetadataType type) {
    return getId(type).get();
  }

  /**
   * Creates a {@code ref} to the abstract element of the given {@code type} based on its {@code typeDsl} declaration. Anywhere
   * this ref element is used, the schema will allow to declare an xml element with a substitution group that matches the
   * referenced abstract-element
   * <p/>
   * For example, if we create this ref element to the type of name {@code TypeName}:
   * <p>
   * <xs:element minOccurs="0" maxOccurs="1" ref="ns:abstract-type-name"></xs:element>
   * <p>
   * <p/>
   * Then, any of the following elements are allowed to be used where the ref exists:
   * <p>
   * <xs:element type="ns:org.mule.test.SomeType" substitutionGroup="ns:abstract-type-name" name="some-type"></xs:element>
   * <p>
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

    QName refQName = new QName(typeDsl.getNamespace(), getAbstractElementName(typeDsl), typeDsl.getPrefix());
    return createRefElement(refQName, isRequired);
  }

  void declareAsParameter(MetadataType type, ExtensionType extensionType, ParameterModel parameterModel,
                          DslElementSyntax paramDsl, List<TopLevelElement> childElements) {

    if (isContent(parameterModel)) {
      childElements.add(generateTextElement(paramDsl, parameterModel.getDescription(), parameterModel.isRequired()));
    } else {
      type.accept(getParameterDeclarationVisitor(extensionType, childElements, parameterModel));
    }
  }

  private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType,
                                                             final List<TopLevelElement> childElements,
                                                             final ParameterModel parameterModel) {
    final DslElementSyntax paramDsl = dslResolver.resolve(parameterModel);
    return getParameterDeclarationVisitor(extensionType, childElements,
                                          parameterModel.getName(), parameterModel.getDescription(),
                                          parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                                          parameterModel.getDefaultValue(), paramDsl,
                                          parameterModel.getDslConfiguration());
  }

  private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType,
                                                             final List<TopLevelElement> childElements,
                                                             final String name, final String description,
                                                             ExpressionSupport expressionSupport, boolean required,
                                                             Object defaultValue, DslElementSyntax paramDsl,
                                                             ParameterDslConfiguration dslModel) {
    return new MetadataTypeVisitor() {

      private boolean forceOptional = paramDsl.supportsChildDeclaration() || !required;

      @Override
      public void visitArrayType(ArrayType arrayType) {
        defaultVisit(arrayType);
        if (paramDsl.supportsChildDeclaration()) {
          collectionDelegate.generateCollectionElement(arrayType, paramDsl, description,
                                                       !paramDsl.supportsAttributeDeclaration(),
                                                       childElements);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        defaultVisit(objectType);
        if (isMap(objectType)) {
          if (paramDsl.supportsChildDeclaration()) {
            mapDelegate.generateMapElement(objectType, paramDsl, description,
                                           !paramDsl.supportsAttributeDeclaration(),
                                           childElements);
          }
        } else {
          objectTypeDelegate.generatePojoElement(objectType, paramDsl, dslModel, description, childElements, required);
        }
      }

      @Override
      public void visitString(StringType stringType) {
        if (paramDsl.supportsChildDeclaration()) {
          childElements.add(generateTextElement(paramDsl, description, isRequired(forceOptional, required)));
        } else {
          defaultVisit(stringType);
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (paramDsl.supportsAttributeDeclaration()) {
          extensionType.getAttributeOrAttributeGroup().add(createAttribute(name, description, metadataType, defaultValue,
                                                                           isRequired(forceOptional, required),
                                                                           expressionSupport));
        }
      }

      private boolean isRequired(boolean forceOptional, boolean required) {
        return !forceOptional && required;
      }
    };
  }

  private XmlDslModel registerExtensionImport(ExtensionModel extension) {
    XmlDslModel languageModel = extension.getXmlDslModel();
    Import schemaImport = new Import();
    schemaImport.setNamespace(languageModel.getNamespace());
    schemaImport.setSchemaLocation(languageModel.getSchemaLocation());
    if (!schema.getIncludeOrImportOrRedefine().contains(schemaImport)) {
      schema.getIncludeOrImportOrRedefine().add(schemaImport);
    }

    return languageModel;
  }

  void addTlsSupport(ExtensionType extensionType) {
    if (!requiresTls) {
      importTlsNamespace();
      requiresTls = true;
    }
    extensionType.getAttributeOrAttributeGroup()
        .add(createAttribute(TLS_PARAMETER_NAME, load(String.class), false, NOT_SUPPORTED));
  }

  void addTlsSupport(ExtensionType extensionType, List<TopLevelElement> childElements) {
    addTlsSupport(extensionType);
    childElements.add(createRefElement(TLS_CONTEXT_TYPE, false));
  }

  TopLevelElement createRefElement(QName elementRef, boolean isRequired) {
    TopLevelElement element = new TopLevelElement();
    element.setRef(elementRef);
    element.setMinOccurs(isRequired ? ONE : ZERO);
    element.setMaxOccurs(MAX_ONE);
    return element;
  }

  NamedGroup createGroup(QName elementRef, boolean isRequired) {
    NamedGroup namedGroup = new NamedGroup();
    namedGroup.setRef(elementRef);
    namedGroup.setMinOccurs(isRequired ? ONE : ZERO);
    return namedGroup;
  }

  Attribute createAttribute(String name, String description, boolean optional, QName type) {
    Attribute attr = new Attribute();
    attr.setName(name);
    attr.setUse(optional ? USE_OPTIONAL : USE_REQUIRED);
    attr.setType(type);

    if (description != null) {
      attr.setAnnotation(createDocAnnotation(description));
    }

    return attr;
  }

  Schema getSchema() {
    return schema;
  }

  TypeCatalog getTypesMapping() {
    return typesMapping;
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

  ObjectTypeSchemaDelegate getObjectSchemaDelegate() {
    return objectTypeDelegate;
  }

  ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  private TopLevelElement generateTextElement(DslElementSyntax paramDsl, String description, boolean isRequired) {
    TopLevelElement textElement = createTopLevelElement(paramDsl.getElementName(), isRequired ? ONE : ZERO, MAX_ONE);
    textElement.setAnnotation(createDocAnnotation(description));
    textElement.setType(STRING);

    return textElement;
  }

  boolean isRequired(TopLevelElement element) {
    return element.getMinOccurs().equals(ONE);
  }

  void addParameterToSequence(List<TopLevelElement> parameters, ExplicitGroup sequence) {
    parameters.forEach(parameter -> {
      sequence.getParticle().add(objectFactory.createElement(parameter));
      if (isRequired(parameter)) {
        sequence.setMinOccurs(ONE);
      }
    });
  }

  void addInfrastructureParameters(ExtensionType extensionType, ParameterizedModel model, ExplicitGroup sequence) {
    model.getAllParameterModels().stream()
        .filter(p -> p.getModelProperty(InfrastructureParameterModelProperty.class).isPresent())
        .sorted(comparing(p -> p.getModelProperty(InfrastructureParameterModelProperty.class).get().getSequence()))
        .forEach(parameter -> {
          parameter.getModelProperty(QNameModelProperty.class)
              .map(QNameModelProperty::getValue)
              .ifPresent(qName -> sequence.getParticle()
                  .add(objectFactory.createElement(createRefElement(qName, false))));
          if (parameter.getName().equals(TLS_PARAMETER_NAME)) {
            addTlsSupport(extensionType);
          }
        });
  }

  void addInlineParameterGroup(ParameterGroupModel group, ExplicitGroup parentSequence) {
    DslElementSyntax groupDsl = dslResolver.resolveInline(group);

    LocalComplexType complexType = objectTypeDelegate.createTypeExtension(MULE_ABSTRACT_EXTENSION_TYPE);
    ExplicitGroup groupSequence = new ExplicitGroup();

    List<ParameterModel> groupParameters = group.getParameterModels();
    List<TopLevelElement> parameterElements =
        registerParameters(complexType.getComplexContent().getExtension(), groupParameters);
    addParameterToSequence(parameterElements, groupSequence);

    BigInteger minOccurs = ExtensionModelUtils.isRequired(group) ? ONE : ZERO;
    TopLevelElement groupElement = createTopLevelElement(groupDsl.getElementName(), minOccurs, MAX_ONE);
    groupElement.setComplexType(complexType);

    complexType.getComplexContent().getExtension().setSequence(groupSequence);
    parentSequence.getParticle().add(objectFactory.createElement(groupElement));
  }

  QName resolveSubstitutionGroup(SubstitutionGroup userConfiguredSubstitutionGroup) {
    String namespaceUri = getNamespaceUri(userConfiguredSubstitutionGroup.getPrefix());
    return new QName(namespaceUri, userConfiguredSubstitutionGroup.getElement(), userConfiguredSubstitutionGroup.getPrefix());
  }

  String getNamespaceUri(String prefix) {
    if (prefix.equals(CORE_PREFIX)) {
      importIfNotImported(createMuleImport());
      return CORE_NAMESPACE;
    }
    if (prefix.equals(EE_PREFIX)) {
      importIfNotImported(createMuleEEImport());
      return EE_NAMESPACE;
    }
    Optional<ExtensionModel> extensionModelFromPrefix = dslContext.getExtensions().stream()
        .filter((extensionModel) -> prefix.equals(extensionModel.getXmlDslModel().getPrefix())).findFirst();
    if (extensionModelFromPrefix.isPresent()) {
      registerExtensionImport(extensionModelFromPrefix.get());
      return extensionModelFromPrefix.get().getXmlDslModel().getNamespace();
    }
    throw new IllegalArgumentException(String
        .format("prefix: %s inside substitutionGroup does not exist. It does not relate to any imported namespaces",
                prefix));
  }
}
