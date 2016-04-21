/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_NAMESPACE;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.capability.xml.XmlModelUtils.createXmlModelProperty;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_KEY;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.CONFIG_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.CONFIG_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_THREADING_PROFILE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.OPERATION_SUBSTITUTION_GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.TLS_CONTEXT_TYPE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.XML_NAMESPACE;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAliasName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isRequired;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.utils.MetadataTypeUtils;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParametrizedModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Annotation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Documentation;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.FormChoice;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.GroupRef;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Import;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NoFixedFacet;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Restriction;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaTypeConversion;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelSimpleType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Union;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;
import org.mule.runtime.module.extension.internal.model.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.runtime.module.extension.internal.util.NameUtils;

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

import org.springframework.util.ClassUtils;

/**
 * Builder class to generate a XSD schema that describes a
 * {@link ExtensionModel}
 *
 * @since 3.7.0
 */
public final class SchemaBuilder
{

    private static final String UNBOUNDED = "unbounded";

    private final Set<Class<? extends Enum>> registeredEnums = new LinkedHashSet<>();
    private final Map<Class<?>, ComplexTypeHolder> registeredComplexTypesHolders = new LinkedHashMap<>();
    private final Map<String, NamedGroup> substitutionGroups = new LinkedHashMap<>();
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private final ConfigurationSchemaDelegate configurationSchemaDelegate = new ConfigurationSchemaDelegate(this);
    private final ConnectionProviderSchemaDelegate connectionProviderSchemaDelegate = new ConnectionProviderSchemaDelegate(this);
    private final OperationSchemaDelegate operationSchemaDelegate = new OperationSchemaDelegate(this);
    private final SourceSchemaDelegate sourceSchemaDelegate = new SourceSchemaDelegate(this);

    private Schema schema;
    private boolean requiresTls = false;
    private SubTypesMappingContainer subTypesMapping;
    private Map<MetadataType, MetadataType> importedTypes;

    public static SchemaBuilder newSchema(ExtensionModel extensionModel, String targetNamespace)
    {
        SchemaBuilder builder = new SchemaBuilder();
        builder.schema = new Schema();
        builder.schema.setTargetNamespace(targetNamespace);
        builder.schema.setElementFormDefault(FormChoice.QUALIFIED);
        builder.schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);
        builder.importXmlNamespace()
                .importSpringFrameworkNamespace()
                .importMuleNamespace()
                .importMuleExtensionNamespace();

        Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
        builder.withTypeMapping(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : ImmutableMap.of());

        Optional<Map<MetadataType, MetadataType>> importedTypes = extensionModel.getModelProperty(ImportedTypesModelProperty.class).map(ImportedTypesModelProperty::getImportedTypes);
        builder.withImportedTypes(importedTypes.isPresent() ? importedTypes.get() : ImmutableMap.of());

        return builder;
    }

    private SchemaBuilder withTypeMapping(Map<MetadataType, List<MetadataType>> subTypesMapping)
    {
        this.subTypesMapping = new SubTypesMappingContainer(subTypesMapping);
        return this;
    }

    private SchemaBuilder withImportedTypes(Map<MetadataType, MetadataType> importedTypes)
    {
        this.importedTypes = importedTypes;
        return this;
    }

    public Schema build()
    {
        return schema;
    }

    private SchemaBuilder importXmlNamespace()
    {
        Import xmlImport = new Import();
        xmlImport.setNamespace(XML_NAMESPACE);
        schema.getIncludeOrImportOrRedefine().add(xmlImport);
        return this;
    }

    private SchemaBuilder importSpringFrameworkNamespace()
    {
        Import springFrameworkImport = new Import();
        springFrameworkImport.setNamespace(SPRING_FRAMEWORK_NAMESPACE);
        springFrameworkImport.setSchemaLocation(SPRING_FRAMEWORK_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(springFrameworkImport);
        return this;
    }

    private SchemaBuilder importMuleNamespace()
    {
        Import muleSchemaImport = new Import();
        muleSchemaImport.setNamespace(MULE_NAMESPACE);
        muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
        return this;
    }

    private SchemaBuilder importMuleExtensionNamespace()
    {
        Import muleExtensionImport = new Import();
        muleExtensionImport.setNamespace(MULE_EXTENSION_NAMESPACE);
        muleExtensionImport.setSchemaLocation(MULE_EXTENSION_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleExtensionImport);

        return this;
    }

    private SchemaBuilder importTlsNamespace()
    {
        Import tlsImport = new Import();
        tlsImport.setNamespace(MULE_TLS_NAMESPACE);
        tlsImport.setSchemaLocation(MULE_TLS_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(tlsImport);

        return this;
    }

    protected void addRetryPolicy(ExplicitGroup sequence)
    {
        TopLevelElement providerElementRetry = createRefElement(MULE_ABSTRACT_RECONNECTION_STRATEGY, false);
        sequence.getParticle().add(objectFactory.createElement(providerElementRetry));
    }

    public SchemaBuilder registerConnectionProviderElement(ConnectionProviderModel providerModel)
    {
        connectionProviderSchemaDelegate.registerConnectionProviderElement(schema, providerModel);
        return this;
    }

    public SchemaBuilder registerConfigElement(final RuntimeConfigurationModel configurationModel)
    {
        configurationSchemaDelegate.registerConfigElement(schema, configurationModel);
        return this;
    }

    Attribute createNameAttribute(boolean required)
    {
        return createAttribute(SchemaConstants.ATTRIBUTE_NAME_NAME, load(String.class), required, NOT_SUPPORTED);
    }

    public SchemaBuilder registerOperation(OperationModel operationModel)
    {
        operationSchemaDelegate.registerOperation(schema, operationModel);
        return this;
    }

    public SchemaBuilder registerMessageSource(SourceModel sourceModel)
    {

        sourceSchemaDelegate.registerMessageSource(schema, sourceModel);
        return this;
    }


    void registerParameters(ExtensionType type, ExplicitGroup choice, Collection<ParameterModel> parameterModels)
    {
        for (final ParameterModel parameterModel : getSortedParameterModels(parameterModels))
        {
            parameterModel.getType().accept(getParameterDeclarationVisitor(type, choice, parameterModel));
        }

        if (!choice.getParticle().isEmpty())
        {
            type.setSequence(choice);
        }
    }

    /**
     * Sorts the given {@code parameterModels} so that infrastructure ones are firsts and the
     * rest maintain their relative order. If more than one infrastructure parameter
     * is found, the subgroup is sorted alphabetically.
     * <p>
     * The {@link InfrastructureParameterModelProperty} is used to identify those
     * parameters which are infrastructure
     *
     * @param parameterModels a {@link Collection} of {@link ParameterModel parameter models}
     * @return a sorted {@link List}
     */
    private List<ParameterModel> getSortedParameterModels(Collection<ParameterModel> parameterModels)
    {
        List<ParameterModel> sortedParameters = new ArrayList<>(parameterModels);
        sortedParameters.sort((left, right) -> {
            boolean isLeftInfrastructure = left.getModelProperty(InfrastructureParameterModelProperty.class).isPresent();
            boolean isRightInfrastructure = right.getModelProperty(InfrastructureParameterModelProperty.class).isPresent();

            if (!isLeftInfrastructure && !isRightInfrastructure)
            {
                return 0;
            }

            if (!isLeftInfrastructure && isRightInfrastructure)
            {
                return 1;
            }

            if (isLeftInfrastructure && !isRightInfrastructure)
            {
                return -1;
            }

            return left.getName().compareTo(right.getName());
        });
        return sortedParameters;
    }

    /**
     * Registers a pojo type creating a base complex type and a substitutable
     * top level type while assigning it a name. This method will not register
     * the same type twice even if requested to
     *
     * @param metadataType a {@link ObjectType} describing a pojo type
     * @param description  the type's description
     * @return the reference name of the complexType
     */
    private String registerPojoType(ObjectType metadataType, String description)
    {
        ComplexTypeHolder alreadyRegisteredType = registeredComplexTypesHolders.get(getType(metadataType));
        if (alreadyRegisteredType != null)
        {
            return alreadyRegisteredType.getComplexType().getName();
        }

        registerBasePojoType(metadataType, description);
        registerPojoGlobalElement(metadataType, description);

        return getBaseTypeName(metadataType);
    }

    private String getBaseTypeName(MetadataType type)
    {
        return NameUtils.sanitizeName(getType(type).getName());
    }

    private TopLevelComplexType registerBasePojoType(ObjectType metadataType, String description)
    {
        final TopLevelComplexType complexType = new TopLevelComplexType();
        final Class<?> clazz = getType(metadataType);
        registeredComplexTypesHolders.put(clazz, new ComplexTypeHolder(complexType, metadataType));

        complexType.setName(NameUtils.sanitizeName(clazz.getName()));
        complexType.setAnnotation(createDocAnnotation(description));

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);

        final ExtensionType extension = new ExtensionType();
        extension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(extension);


        for (ObjectFieldType field : metadataType.getFields())
        {
            final String name = field.getKey().getName().getLocalPart();
            final MetadataType fieldType = field.getValue();
            final Class<?> fieldClass = getType(field);
            final String defaultValue = MetadataTypeUtils.getDefaultValue(metadataType).orElse(null);
            final ExpressionSupport expressionSupport = TypeUtils.getExpressionSupport(field);

            fieldType.accept(new MetadataTypeVisitor()
            {
                @Override
                public void visitArrayType(ArrayType arrayType)
                {
                    final ExplicitGroup all = getOrCreateSequenceGroup(extension);
                    generateCollectionElement(all, name, EMPTY, arrayType, field.isRequired());
                }

                @Override
                public void visitDictionary(DictionaryType dictionaryType)
                {
                    final ExplicitGroup all = getOrCreateSequenceGroup(extension);
                    generateMapElement(all, name, EMPTY, dictionaryType, field.isRequired());
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    if (TlsContextFactory.class.isAssignableFrom(fieldClass))
                    {
                        final ExplicitGroup all = getOrCreateSequenceGroup(extension);
                        addTlsSupport(extension, all);
                        return;
                    }

                    if (shouldGeneratePojoChildElements(fieldClass))
                    {
                        if (ExpressionSupport.REQUIRED != expressionSupport)
                        {
                            final ExplicitGroup all = getOrCreateSequenceGroup(extension);
                            registerComplexTypeChildElement(all, name, EMPTY, objectType, false);
                        }
                        else
                        {
                            defaultVisit(objectType);
                            registerPojoType(objectType, EMPTY);
                        }
                    }
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    Attribute attribute = createAttribute(name, EMPTY, fieldType, defaultValue, field.isRequired(), expressionSupport);
                    extension.getAttributeOrAttributeGroup().add(attribute);
                }
            });
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
        return complexType;
    }

    private ExplicitGroup getOrCreateSequenceGroup(ExtensionType extension)
    {
        ExplicitGroup all = extension.getSequence();
        if (all == null)
        {
            all = new ExplicitGroup();
            extension.setSequence(all);
        }
        return all;
    }

    public SchemaBuilder registerEnums()
    {
        registeredEnums.forEach(enumClass -> registerEnum(schema, enumClass));
        return this;
    }

    private void registerEnum(Schema schema, Class<? extends Enum> enumType)
    {
        TopLevelSimpleType enumSimpleType = new TopLevelSimpleType();
        enumSimpleType.setName(NameUtils.sanitizeName(enumType.getName()) + SchemaConstants.ENUM_TYPE_SUFFIX);

        Union union = new Union();
        union.getSimpleType().add(createEnumSimpleType(enumType));
        union.getSimpleType().add(createExpressionAndPropertyPlaceHolderSimpleType());
        enumSimpleType.setUnion(union);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(enumSimpleType);
    }

    private LocalSimpleType createExpressionAndPropertyPlaceHolderSimpleType()
    {
        LocalSimpleType expression = new LocalSimpleType();
        Restriction restriction = new Restriction();
        expression.setRestriction(restriction);
        restriction.setBase(SchemaConstants.MULE_PROPERTY_PLACEHOLDER_TYPE);

        return expression;
    }

    private LocalSimpleType createEnumSimpleType(Class<? extends Enum> enumClass)
    {
        LocalSimpleType enumValues = new LocalSimpleType();
        Restriction restriction = new Restriction();
        enumValues.setRestriction(restriction);
        restriction.setBase(SchemaConstants.STRING);


        for (Enum value : enumClass.getEnumConstants())
        {
            NoFixedFacet noFixedFacet = objectFactory.createNoFixedFacet();
            noFixedFacet.setValue(value.name());

            JAXBElement<NoFixedFacet> enumeration = objectFactory.createEnumeration(noFixedFacet);
            enumValues.getRestriction().getFacets().add(enumeration);
        }

        return enumValues;
    }

    private void registerComplexTypeChildElement(ExplicitGroup all,
                                                 String name,
                                                 String description,
                                                 ObjectType objectType,
                                                 boolean required)
    {
        name = hyphenize(name);

        // this top level element is for declaring the object inside a config or operation
        TopLevelElement objectElement = createTopLevelElement(name, required ? ONE : ZERO, "1");
        objectElement.setComplexType(newLocalComplexTypeWithBase(objectType, description));
        objectElement.setAnnotation(createDocAnnotation(description));

        all.getParticle().add(objectFactory.createElement(objectElement));
    }

    private void registerPojoGlobalElement(ObjectType metadataType, String description)
    {
        TopLevelElement objectElement = new TopLevelElement();
        objectElement.setName(getTopLevelTypeName(metadataType));

        LocalComplexType complexContent = newLocalComplexTypeWithBase(metadataType, description);
        complexContent.getComplexContent().getExtension().getAttributeOrAttributeGroup().add(createNameAttribute(false));
        objectElement.setComplexType(complexContent);

        objectElement.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);
        objectElement.setAnnotation(createDocAnnotation(description));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(objectElement);
    }

    private LocalComplexType newLocalComplexTypeWithBase(ObjectType type, String description)
    {
        LocalComplexType objectComplexType = new LocalComplexType();
        objectComplexType.setComplexContent(new ComplexContent());
        objectComplexType.getComplexContent().setExtension(new ExtensionType());
        objectComplexType.getComplexContent().getExtension().setBase(
                new QName(schema.getTargetNamespace(), registerPojoType(type, description))
        ); // base to the pojo type

        return objectComplexType;
    }

    private Attribute createAttribute(ParameterModel parameterModel, boolean required)
    {
        return createAttribute(parameterModel.getName(),
                               parameterModel.getDescription(),
                               parameterModel.getType(),
                               parameterModel.getDefaultValue(),
                               required,
                               parameterModel.getExpressionSupport());
    }

    Attribute createAttribute(String name, MetadataType type, boolean required, ExpressionSupport expressionSupport)
    {
        return createAttribute(name, EMPTY, type, null, required, expressionSupport);
    }

    private Attribute createAttribute(final String name, String description, final MetadataType type, Object defaultValue, boolean required, final ExpressionSupport expressionSupport)
    {
        final Attribute attribute = new Attribute();
        attribute.setUse(required ? SchemaConstants.USE_REQUIRED : SchemaConstants.USE_OPTIONAL);
        attribute.setAnnotation(createDocAnnotation(description));

        if (defaultValue instanceof String && StringUtils.isNotBlank(defaultValue.toString()))
        {
            attribute.setDefault(defaultValue.toString());
        }

        type.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitString(StringType stringType)
            {
                Optional<EnumAnnotation> enumAnnotation = getSingleAnnotation(stringType, EnumAnnotation.class);

                if (enumAnnotation.isPresent())
                {
                    visitEnum(stringType);
                }
                else
                {
                    defaultVisit(stringType);
                }
            }

            private void visitEnum(StringType stringType)
            {
                attribute.setName(name);

                Class<? extends Enum> enumType;
                try
                {
                    enumType = getType(stringType);
                }
                catch (Exception e)
                {
                    throw new IllegalParameterModelDefinitionException(String.format("Parameter '%s' refers to an enum class which couldn't be loaded.", name), e);
                }

                attribute.setType(new QName(schema.getTargetNamespace(), NameUtils.sanitizeName(enumType.getName()) + SchemaConstants.ENUM_TYPE_SUFFIX));
                registeredEnums.add(enumType);
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                attribute.setName(name);
                attribute.setType(SchemaTypeConversion.convertType(type, expressionSupport));
            }
        });

        return attribute;
    }

    private void generateCollectionElement(ExplicitGroup all, ParameterModel parameterModel, boolean forceOptional)
    {
        boolean required = isRequired(parameterModel, forceOptional);
        generateCollectionElement(all, parameterModel.getName(), parameterModel.getDescription(), (ArrayType) parameterModel.getType(), required);
    }

    private void generateCollectionElement(ExplicitGroup all, String name, String description, ArrayType metadataType, boolean required)
    {
        name = hyphenize(name);

        BigInteger minOccurs = required ? ONE : ZERO;
        String collectionName = hyphenize(NameUtils.singularize(name));
        LocalComplexType collectionComplexType = generateCollectionComplexType(collectionName, description, metadataType);

        TopLevelElement collectionElement = createTopLevelElement(name, minOccurs, "1");
        collectionElement.setAnnotation(createDocAnnotation(description));
        all.getParticle().add(objectFactory.createElement(collectionElement));

        collectionElement.setComplexType(collectionComplexType);
    }

    private LocalComplexType generateCollectionComplexType(String name, final String description, final ArrayType metadataType)
    {
        final LocalComplexType collectionComplexType = new LocalComplexType();
        final ExplicitGroup sequence = new ExplicitGroup();
        collectionComplexType.setSequence(sequence);

        final TopLevelElement collectionItemElement = createTopLevelElement(name, ZERO, SchemaConstants.UNBOUNDED);

        final MetadataType genericType = metadataType.getType();

        genericType.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                collectionItemElement.setComplexType(newLocalComplexTypeWithBase(objectType, description));
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                LocalComplexType complexType = new LocalComplexType();
                complexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, genericType, true, SUPPORTED));
                collectionItemElement.setComplexType(complexType);
            }
        });

        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

        return collectionComplexType;
    }

    private void generateMapElement(ExplicitGroup all, ParameterModel parameterModel, boolean forceOptional)
    {
        boolean required = isRequired(parameterModel, forceOptional);
        generateMapElement(all, parameterModel.getName(), parameterModel.getDescription(), (DictionaryType) parameterModel.getType(), required);
    }

    private void generateMapElement(ExplicitGroup all, String name, String description, DictionaryType metadataType, boolean required)
    {
        name = hyphenize(name);

        BigInteger minOccurs = required ? ONE : ZERO;
        String mapName = hyphenize(NameUtils.pluralize(name));
        LocalComplexType mapComplexType = generateMapComplexType(mapName, description, metadataType);

        TopLevelElement mapElement = createTopLevelElement(mapName, minOccurs, "1");
        mapElement.setAnnotation(createDocAnnotation(description));
        all.getParticle().add(objectFactory.createElement(mapElement));

        mapElement.setComplexType(mapComplexType);
    }

    private LocalComplexType generateMapComplexType(String name, final String description, final DictionaryType metadataType)
    {
        final LocalComplexType mapComplexType = new LocalComplexType();
        final ExplicitGroup mapEntrySequence = new ExplicitGroup();
        mapComplexType.setSequence(mapEntrySequence);

        final TopLevelElement mapEntryElement = new TopLevelElement();
        mapEntryElement.setName(NameUtils.singularize(name));
        mapEntryElement.setMinOccurs(ZERO);
        mapEntryElement.setMaxOccurs(SchemaConstants.UNBOUNDED);

        final MetadataType keyType = metadataType.getKeyType();
        final MetadataType valueType = metadataType.getValueType();
        final LocalComplexType entryComplexType = new LocalComplexType();
        final Attribute keyAttribute = createAttribute(ATTRIBUTE_NAME_KEY, keyType, true, ExpressionSupport.REQUIRED);
        entryComplexType.getAttributeOrAttributeGroup().add(keyAttribute);

        valueType.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                final boolean shouldGenerateChildElement = shouldGeneratePojoChildElements(getType(objectType));

                entryComplexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, valueType, !shouldGenerateChildElement, SUPPORTED));

                if (shouldGenerateChildElement)
                {
                    ExplicitGroup singleItemSequence = new ExplicitGroup();
                    singleItemSequence.setMaxOccurs("1");

                    LocalComplexType itemComplexType = newLocalComplexTypeWithBase(objectType, description);
                    TopLevelElement itemElement = createTopLevelElement(NameUtils.getTopLevelTypeName(objectType), ZERO, "1", itemComplexType);
                    singleItemSequence.getParticle().add(objectFactory.createElement(itemElement));

                    entryComplexType.setSequence(singleItemSequence);
                }
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                entryComplexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, valueType, false, SUPPORTED));
                entryComplexType.setSequence(new ExplicitGroup());

                LocalComplexType itemComplexType = new LocalComplexType();
                MetadataType itemType = arrayType.getType();
                itemComplexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, itemType, true, REQUIRED));

                String itemName = hyphenize(NameUtils.singularize(name)).concat("-item");
                TopLevelElement itemElement = createTopLevelElement(itemName, ZERO, SchemaConstants.UNBOUNDED, itemComplexType);
                entryComplexType.getSequence().getParticle().add(objectFactory.createElement(itemElement));
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                entryComplexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, valueType, true, SUPPORTED));
            }
        });

        mapEntryElement.setComplexType(entryComplexType);

        mapEntrySequence.getParticle().add(objectFactory.createElement(mapEntryElement));

        return mapComplexType;
    }

    QName getSubstitutionGroup(Class<?> type)
    {
        return new QName(schema.getTargetNamespace(), registerExtensibleElement(type));
    }

    private String registerExtensibleElement(Class<?> type)
    {
        Extensible extensible = type.getAnnotation(Extensible.class);
        checkArgument(extensible != null, String.format("Type %s is not extensible", type.getName()));

        String name = extensible.alias();
        if (StringUtils.isBlank(name))
        {
            name = type.getName() + OPERATION_SUBSTITUTION_GROUP_SUFFIX;
        }

        NamedGroup group = substitutionGroups.get(name);
        if (group == null)
        {
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

    private String getGroupName(String name)
    {
        return name + GROUP_SUFFIX;
    }

    ExtensionType registerExecutableType(String name, ParametrizedModel parametrizedModel, QName base)
    {
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

        for (final ParameterModel parameterModel : parametrizedModel.getParameterModels())
        {
            MetadataType parameterType = parameterModel.getType();

            if (isOperation(parameterType))
            {
                String maxOccurs = parameterType instanceof ArrayType ? UNBOUNDED : "1";
                generateNestedProcessorElement(all, parameterModel, maxOccurs);
            }
            else
            {
                parameterType.accept(getParameterDeclarationVisitor(complexContentExtension, all, parameterModel));
            }
        }

        if (all.getParticle().size() == 0)
        {
            complexContentExtension.setSequence(null);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

        return complexContentExtension;
    }

    private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all, final ParameterModel parameterModel)
    {
        return new MetadataTypeVisitor()
        {
            private boolean forceOptional = false;

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                MetadataType genericType = arrayType.getType();
                forceOptional = shouldForceOptional(getType(genericType));

                defaultVisit(arrayType);
                if (shouldGenerateDataTypeChildElements(genericType, parameterModel))
                {
                    generateCollectionElement(all, parameterModel, true);
                }
            }

            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                MetadataType keyType = dictionaryType.getKeyType();
                forceOptional = shouldForceOptional(getType(keyType));

                defaultVisit(dictionaryType);

                if (shouldGenerateDataTypeChildElements(keyType, parameterModel))
                {
                    generateMapElement(all, parameterModel, true);
                }
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                final Class<?> clazz = getType(objectType);
                forceOptional = shouldForceOptional(clazz);

                if (TlsContextFactory.class.isAssignableFrom(clazz))
                {
                    addTlsSupport(extensionType, all);
                    return;
                }

                if (ThreadingProfile.class.isAssignableFrom(clazz))
                {
                    addAttributeAndElement(extensionType, all, THREADING_PROFILE_ATTRIBUTE_NAME, MULE_ABSTRACT_THREADING_PROFILE);
                    return;
                }

                defaultVisit(objectType);

                if (importedTypes.get(objectType) != null)
                {
                    addImportedTypeRef(getType(importedTypes.get(objectType)), parameterModel, all);
                    return;
                }

                if (ExpressionSupport.REQUIRED != parameterModel.getExpressionSupport())
                {
                    if (shouldGeneratePojoChildElements(clazz))
                    {
                        registerComplexTypeChildElement(all,
                                                        parameterModel.getName(),
                                                        parameterModel.getDescription(),
                                                        objectType,
                                                        false);
                    }
                    else
                    {
                        List<MetadataType> subTypes = subTypesMapping.getSubTypes(parameterModel.getType());
                        if (!subTypes.isEmpty())
                        {
                            registerPojoSubtypes(subTypes, all);
                        }
                    }
                }
                else
                {
                    //We need to register the type, just in case people want to use it as global elements
                    registerPojoType(objectType, parameterModel.getDescription());
                }
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                extensionType.getAttributeOrAttributeGroup().add(createAttribute(parameterModel, isRequired(parameterModel, forceOptional)));
            }

            private boolean shouldGenerateDataTypeChildElements(MetadataType metadataType, ParameterModel parameterModel)
            {
                if (metadataType == null)
                {
                    return false;
                }

                boolean isExpressionRequired = ExpressionSupport.REQUIRED == parameterModel.getExpressionSupport();
                boolean isPojo = metadataType instanceof ObjectType;
                Class<?> clazz = getType(metadataType);
                boolean isPrimitive = clazz.isPrimitive() || ClassUtils.isPrimitiveWrapper(clazz);

                return !isExpressionRequired && (isPrimitive || (isPojo && shouldGeneratePojoChildElements(clazz)) || (!isPojo && isInstantiable(clazz)));
            }

            private boolean shouldGeneratePojoChildElements(Class<?> type)
            {
                return IntrospectionUtils.isInstantiable(type) && !getExposedFields(type).isEmpty();
            }

            private boolean shouldForceOptional(Class<?> type)
            {
                return !parameterModel.isRequired() ||
                       !subTypesMapping.getSubTypes(parameterModel.getType()).isEmpty() ||
                       (IntrospectionUtils.isInstantiable(type) && ExpressionSupport.REQUIRED != parameterModel.getExpressionSupport());
            }
        };
    }

    private void addImportedTypeRef(Class<?> extensionType, ParameterModel parameterModel, ExplicitGroup all)
    {
        XmlModelProperty xml = createXmlModelProperty(getAnnotation(extensionType, Xml.class),
                                                      getAnnotation(extensionType, Extension.class).name(), "");

        Import schemaImport = new Import();
        schemaImport.setNamespace(xml.getNamespaceUri());
        schemaImport.setSchemaLocation(xml.getSchemaLocation());

        schema.getIncludeOrImportOrRedefine().add(schemaImport);

        QName qName = new QName(xml.getNamespaceUri(), hyphenize(getAliasName(parameterModel.getType())), xml.getNamespace());
        all.getParticle().add(objectFactory.createElement(createRefElement(qName, false)));
    }

    private void registerPojoSubtypes(List<MetadataType> subTypes, ExplicitGroup all)
    {
        ExplicitGroup choice = new ExplicitGroup();
        choice.setMinOccurs(ZERO);
        choice.setMaxOccurs("1");

        subTypes.forEach(subtype -> {
            TopLevelElement subtypeElement = createTopLevelElement(hyphenize(getAliasName(subtype)), ZERO, "1");
            subtypeElement.setComplexType(newLocalComplexTypeWithBase((ObjectType) subtype, EMPTY));
            choice.getParticle().add(objectFactory.createElement(subtypeElement));
        });

        all.getParticle().add(objectFactory.createChoice(choice));
    }

    private boolean isOperation(MetadataType type)
    {
        ValueHolder<Boolean> isOperation = new ValueHolder<>(false);
        type.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                if (NestedProcessor.class.isAssignableFrom(getType(objectType)))
                {
                    isOperation.set(true);
                }
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                arrayType.getType().accept(this);
            }
        });

        return isOperation.get();
    }

    private void addTlsSupport(ExtensionType extensionType, ExplicitGroup all)
    {
        if (!requiresTls)
        {
            importTlsNamespace();
            requiresTls = true;
        }

        addAttributeAndElement(extensionType, all, TLS_ATTRIBUTE_NAME, TLS_CONTEXT_TYPE);
    }

    private void addAttributeAndElement(ExtensionType extensionType, ExplicitGroup all, String attributeName, QName elementRef)
    {

        extensionType.getAttributeOrAttributeGroup().add(createAttribute(attributeName,
                                                                         load(String.class),
                                                                         false,
                                                                         ExpressionSupport.NOT_SUPPORTED));

        all.getParticle().add(objectFactory.createElement(createRefElement(elementRef, false)));
    }

    TopLevelElement createRefElement(QName elementRef, boolean isRequired)
    {
        TopLevelElement element = new TopLevelElement();
        element.setRef(elementRef);
        element.setMinOccurs(isRequired ? ONE : ZERO);
        element.setMaxOccurs("1");
        return element;
    }

    private boolean shouldGeneratePojoChildElements(Class<?> type)
    {
        return IntrospectionUtils.isInstantiable(type) && !getExposedFields(type).isEmpty();
    }

    private void generateNestedProcessorElement(ExplicitGroup all, ParameterModel parameterModel, String maxOccurs)
    {
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

    private GroupRef generateNestedProcessorGroup(ParameterModel parameterModel, String maxOccurs)
    {
        QName ref = MULE_MESSAGE_PROCESSOR_TYPE;
        TypeRestrictionModelProperty restrictionCapability = parameterModel.getModelProperty(TypeRestrictionModelProperty.class).orElse(null);
        if (restrictionCapability != null)
        {
            ref = getSubstitutionGroup(restrictionCapability.getType());
            ref = new QName(ref.getNamespaceURI(), getGroupName(ref.getLocalPart()), ref.getPrefix());
        }

        GroupRef group = new GroupRef();
        group.setRef(ref);
        group.setMinOccurs(parameterModel.isRequired() ? ONE : ZERO);
        group.setMaxOccurs(maxOccurs);

        return group;
    }

    private Attribute createAttribute(String name, String description, boolean optional, QName type)
    {
        Attribute attr = new Attribute();
        attr.setName(name);
        attr.setUse(optional ? SchemaConstants.USE_OPTIONAL : SchemaConstants.USE_REQUIRED);
        attr.setType(type);

        if (description != null)
        {
            attr.setAnnotation(createDocAnnotation(description));
        }

        return attr;
    }

    Annotation createDocAnnotation(String content)
    {
        if (StringUtils.isBlank(content))
        {
            return null;
        }

        Annotation annotation = new Annotation();
        Documentation doc = new Documentation();
        doc.getContent().add(content);
        annotation.getAppinfoOrDocumentation().add(doc);
        return annotation;
    }

    MetadataType load(Class<?> clazz)
    {
        return typeLoader.load(clazz);
    }

    private TopLevelElement createTopLevelElement(String name, BigInteger minOccurs, String maxOccurs)
    {
        TopLevelElement element = new TopLevelElement();
        element.setName(name);
        element.setMinOccurs(minOccurs);
        element.setMaxOccurs(maxOccurs);
        return element;
    }

    private TopLevelElement createTopLevelElement(String name, BigInteger minOccurs, String maxOccurs, LocalComplexType type)
    {
        TopLevelElement element = createTopLevelElement(name, minOccurs, maxOccurs);
        element.setComplexType(type);

        return element;
    }

    private class ComplexTypeHolder
    {

        private ComplexType complexType;
        private MetadataType type;

        public ComplexTypeHolder(ComplexType complexType, MetadataType type)
        {
            this.complexType = complexType;
            this.type = type;
        }

        public ComplexType getComplexType()
        {
            return complexType;
        }

        public MetadataType getType()
        {
            return type;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ComplexTypeHolder)
            {
                ComplexTypeHolder other = (ComplexTypeHolder) obj;
                return type.equals(other.getType());
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            return type.hashCode();
        }
    }
}
