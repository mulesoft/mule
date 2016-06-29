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
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.hyphenize;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.sanitizeName;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.singularize;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiableWithParameters;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_KEY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_THREADING_PROFILE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_EXTENSION_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.OPERATION_SUBSTITUTION_GROUP_SUFFIX;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.TLS_CONTEXT_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.XML_NAMESPACE;
import static org.mule.runtime.module.extension.internal.xml.XmlModelUtils.createXmlModelProperty;
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
import org.mule.runtime.config.spring.dsl.api.xml.NameUtils;
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
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
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
    private static final String ABSTRACT_ELEMENT_MASK = "abstract-%s";

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
    private XmlModelProperty xmlProperties;

    public static SchemaBuilder newSchema(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty)
    {
        SchemaBuilder builder = new SchemaBuilder();
        builder.schema = new Schema();
        builder.schema.setTargetNamespace(xmlModelProperty.getNamespaceUri());
        builder.schema.setElementFormDefault(FormChoice.QUALIFIED);
        builder.schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);
        builder.importXmlNamespace()
                .importSpringFrameworkNamespace()
                .importMuleNamespace()
                .importMuleExtensionNamespace();
        builder.withXmlDeclaration(xmlModelProperty);

        Optional<Map<MetadataType, MetadataType>> importedTypes = extensionModel.getModelProperty(ImportedTypesModelProperty.class).map(ImportedTypesModelProperty::getImportedTypes);
        builder.withImportedTypes(importedTypes.orElse(ImmutableMap.of()));

        Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
        builder.withTypeMapping(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : ImmutableMap.of());

        extensionModel.getModelProperty(ExportModelProperty.class).ifPresent(e -> builder.withExportedTypes(e.getExportedClasses()));


        return builder;
    }

    private SchemaBuilder withTypeMapping(Map<MetadataType, List<MetadataType>> subTypesMapping)
    {
        this.subTypesMapping = new SubTypesMappingContainer(subTypesMapping);
        subTypesMapping.forEach(this::registerPojoSubtypes);

        return this;
    }

    private SchemaBuilder withImportedTypes(Map<MetadataType, MetadataType> importedTypes)
    {
        this.importedTypes = importedTypes;
        return this;
    }

    private SchemaBuilder withExportedTypes(List<Class> exportedTypes)
    {
        exportedTypes.stream()
                .filter(IntrospectionUtils::isInstantiableWithParameters)
                .map(c -> getMetadataType(c, ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(c.getClassLoader())))
                .forEach(t -> registerPojoType(t, t.getDescription().orElse(EMPTY)));
        return this;
    }

    public Schema build()
    {
        return schema;
    }

    private SchemaBuilder withXmlDeclaration(XmlModelProperty xmlModelProperty)
    {
        this.xmlProperties = xmlModelProperty;
        return this;
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
        connectionProviderSchemaDelegate.registerConnectionProviderElement(providerModel);
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
        operationSchemaDelegate.registerOperation(operationModel);
        return this;
    }

    public SchemaBuilder registerMessageSource(SourceModel sourceModel)
    {

        sourceSchemaDelegate.registerMessageSource(sourceModel);
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

    private String registerPojoType(MetadataType metadataType, String description)
    {
        return registerPojoType(metadataType, null, description);
    }

    /**
     * Registers a pojo type creating a base complex type and a substitutable
     * top level type while assigning it a name. This method will not register
     * the same type twice even if requested to
     *
     * @param metadataType a {@link MetadataType} describing a pojo type
     * @param metadataType a {@link MetadataType} describing a pojo's base type
     * @param description  the type's description
     * @return the reference name of the complexType
     */
    String registerPojoType(MetadataType metadataType, MetadataType baseType, String description)
    {
        ComplexTypeHolder alreadyRegisteredType = registeredComplexTypesHolders.get(getType(metadataType));
        if (alreadyRegisteredType != null)
        {
            return alreadyRegisteredType.getComplexType().getName();
        }

        registerPojoAsTypeWithBase((ObjectType) metadataType, (ObjectType) baseType, description);
        registerPojoGlobalElement((ObjectType) metadataType, (ObjectType) baseType, description);

        return getBaseTypeName(metadataType);
    }

    private String getBaseTypeName(MetadataType type)
    {
        return sanitizeName(getType(type).getName());
    }

    private ComplexType registerPojoAsTypeWithBase(ObjectType metadataType, ObjectType baseType, String description)
    {
        final Class<?> clazz = getType(metadataType);
        if (registeredComplexTypesHolders.get(clazz) != null)
        {
            return registeredComplexTypesHolders.get(clazz).getComplexType();
        }

        ComplexType complexType;
        if (baseType == null)
        {
            complexType = declarePojoAsType(clazz, metadataType, MULE_ABSTRACT_EXTENSION_TYPE, description, metadataType.getFields());
            complexType.getComplexContent().getExtension().getAttributeOrAttributeGroup().add(createNameAttribute(false));
        }
        else
        {
            XmlModelProperty baseTypeXml = getTypeXmlProperties(baseType);

            QName base = new QName(baseTypeXml.getNamespaceUri(), getBaseTypeName(baseType), baseTypeXml.getNamespace());
            Collection<ObjectFieldType> fields = metadataType.getFields().stream()
                    .filter(f -> !baseType.getFields().contains(f)).collect(toList());

            complexType = declarePojoAsType(clazz, metadataType, base, description, fields);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
        return complexType;
    }

    private XmlModelProperty getTypeXmlProperties(ObjectType baseType)
    {
        return importedTypes.get(baseType) == null ? xmlProperties
                                                   : registerExtensionImport(getType(importedTypes.get(baseType)));
    }

    private ComplexType declarePojoAsType(Class<?> clazz, ObjectType metadataType, QName base, String description, Collection<ObjectFieldType> fields)
    {
        final TopLevelComplexType complexType = new TopLevelComplexType();
        registeredComplexTypesHolders.put(clazz, new ComplexTypeHolder(complexType, metadataType));

        complexType.setName(sanitizeName(clazz.getName()));
        complexType.setAnnotation(createDocAnnotation(description));

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);

        final ExtensionType extension = new ExtensionType();
        extension.setBase(base);

        complexContent.setExtension(extension);

        for (ObjectFieldType field : fields)
        {
            final ExplicitGroup all = getOrCreateSequenceGroup(extension);
            field.getValue().accept(getParameterDeclarationVisitor(extension, all, field));

            if (all.getParticle().isEmpty())
            {
                extension.setSequence(null);
            }
        }

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
        enumSimpleType.setName(sanitizeName(enumType.getName()) + SchemaConstants.ENUM_TYPE_SUFFIX);

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

    private void registerPojoGlobalElement(ObjectType metadataType, ObjectType baseType, String description)
    {
        String abstractElementName = getTopLevelAbstractTypeName(metadataType);

        TopLevelElement abstractElement = new TopLevelElement();
        abstractElement.setName(abstractElementName);

        XmlModelProperty baseTypeXml = getTypeXmlProperties(baseType);
        abstractElement.setSubstitutionGroup(baseType == null ? MULE_ABSTRACT_EXTENSION :
                                             new QName(baseTypeXml.getNamespaceUri(), getTopLevelAbstractTypeName(baseType), baseTypeXml.getNamespace()));
        abstractElement.setAbstract(true);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(abstractElement);

        QName typeRef = new QName(schema.getTargetNamespace(), registerPojoType(metadataType, description), xmlProperties.getNamespace());
        if (!isInstantiableWithParameters(getType(metadataType)))
        {
            abstractElement.setType(typeRef);
        }
        else
        {
            TopLevelElement objectElement = new TopLevelElement();
            objectElement.setName(getTopLevelTypeName(metadataType));
            objectElement.setType(typeRef);
            objectElement.setSubstitutionGroup(new QName(xmlProperties.getNamespaceUri(), abstractElementName, xmlProperties.getNamespace()));
            objectElement.setAnnotation(createDocAnnotation(description));
            schema.getSimpleTypeOrComplexTypeOrGroup().add(objectElement);
        }
    }

    private String getTopLevelAbstractTypeName(MetadataType type)
    {
        return String.format(ABSTRACT_ELEMENT_MASK, getTopLevelTypeName(type));
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

                attribute.setType(new QName(schema.getTargetNamespace(), sanitizeName(enumType.getName()) + SchemaConstants.ENUM_TYPE_SUFFIX));
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

    private void generateCollectionElement(ExplicitGroup all, String name, String description, ArrayType metadataType, boolean required)
    {
        name = hyphenize(name);

        BigInteger minOccurs = required ? ONE : ZERO;
        String collectionName = hyphenize(singularize(name));
        LocalComplexType collectionComplexType = generateCollectionComplexType(collectionName, metadataType);

        TopLevelElement collectionElement = createTopLevelElement(name, minOccurs, "1");
        collectionElement.setAnnotation(createDocAnnotation(description));
        all.getParticle().add(objectFactory.createElement(collectionElement));

        collectionElement.setComplexType(collectionComplexType);
    }

    private LocalComplexType generateCollectionComplexType(String name, final ArrayType metadataType)
    {
        final LocalComplexType collectionComplexType = new LocalComplexType();
        final ExplicitGroup sequence = new ExplicitGroup();
        collectionComplexType.setSequence(sequence);

        final MetadataType genericType = metadataType.getType();

        genericType.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                TopLevelElement collectionItemElement = createRefToLocalElement(objectType);
                collectionItemElement.setMaxOccurs(UNBOUNDED);
                sequence.getParticle().add(objectFactory.createElement(collectionItemElement));
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                LocalComplexType complexType = new LocalComplexType();
                complexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, genericType, true, SUPPORTED));
                TopLevelElement collectionItemElement = createTopLevelElement(name, ZERO, SchemaConstants.UNBOUNDED);
                collectionItemElement.setComplexType(complexType);
                sequence.getParticle().add(objectFactory.createElement(collectionItemElement));
            }
        });

        return collectionComplexType;
    }

    private void generateMapElement(ExplicitGroup all, String name, String description, DictionaryType metadataType, boolean required)
    {
        name = hyphenize(name);

        BigInteger minOccurs = required ? ONE : ZERO;
        String mapName = hyphenize(NameUtils.pluralize(name));
        LocalComplexType mapComplexType = generateMapComplexType(mapName, metadataType);

        TopLevelElement mapElement = createTopLevelElement(mapName, minOccurs, "1");
        mapElement.setAnnotation(createDocAnnotation(description));
        all.getParticle().add(objectFactory.createElement(mapElement));

        mapElement.setComplexType(mapComplexType);
    }

    private LocalComplexType generateMapComplexType(String name, final DictionaryType metadataType)
    {
        final LocalComplexType mapComplexType = new LocalComplexType();
        final ExplicitGroup mapEntrySequence = new ExplicitGroup();
        mapComplexType.setSequence(mapEntrySequence);

        final TopLevelElement mapEntryElement = new TopLevelElement();
        mapEntryElement.setName(singularize(name));
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
                final boolean shouldGenerateChildElement = shouldGenerateDataTypeChildElements(objectType, SUPPORTED);

                entryComplexType.getAttributeOrAttributeGroup().add(createAttribute(ATTRIBUTE_NAME_VALUE, valueType, !shouldGenerateChildElement, SUPPORTED));

                if (shouldGenerateChildElement)
                {
                    ExplicitGroup singleItemSequence = new ExplicitGroup();
                    singleItemSequence.setMaxOccurs("1");

                    singleItemSequence.getParticle().add(objectFactory.createElement(createRefToLocalElement(objectType)));

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

                String itemName = hyphenize(singularize(name)).concat("-item");
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

    ExtensionType registerExecutableType(String name, ParameterizedModel parameterizedModel, QName base)
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

        for (final ParameterModel parameterModel : parameterizedModel.getParameterModels())
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

        if (all.getParticle().isEmpty())
        {
            complexContentExtension.setSequence(null);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

        return complexContentExtension;
    }

    private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType,
                                                               final ExplicitGroup all, final ParameterModel parameterModel)
    {
        return getParameterDeclarationVisitor(extensionType, all, parameterModel.getName(), parameterModel.getDescription(),
                                              parameterModel.getType(), parameterModel.getExpressionSupport(),
                                              parameterModel.isRequired(), parameterModel.getDefaultValue());
    }

    private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType,
                                                               final ExplicitGroup all, final ObjectFieldType field)
    {
        final String name = field.getKey().getName().getLocalPart();
        final MetadataType fieldType = field.getValue();
        final String defaultValue = MetadataTypeUtils.getDefaultValue(fieldType).orElse(null);
        final ExpressionSupport expressionSupport = TypeUtils.getExpressionSupport(field);

        return getParameterDeclarationVisitor(extensionType, all, name, EMPTY, fieldType,
                                              expressionSupport, field.isRequired(), defaultValue);
    }

    private MetadataTypeVisitor getParameterDeclarationVisitor(final ExtensionType extensionType, final ExplicitGroup all,
                                                               final String name, final String description, final MetadataType metadataType,
                                                               ExpressionSupport expressionSupport, boolean required, Object defaultValue)
    {
        return new MetadataTypeVisitor()
        {
            private boolean forceOptional = false;

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                MetadataType genericType = arrayType.getType();
                final boolean supportsChildElement = shouldGenerateDataTypeChildElements(genericType, expressionSupport);
                forceOptional = supportsChildElement || shouldForceOptional(genericType);

                defaultVisit(arrayType);
                if (supportsChildElement)
                {
                    generateCollectionElement(all, name, description, arrayType, isRequired(true, required));
                }
            }

            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                MetadataType keyType = dictionaryType.getKeyType();
                forceOptional = shouldForceOptional(keyType);

                defaultVisit(dictionaryType);
                if (shouldGenerateDataTypeChildElements(keyType, expressionSupport))
                {
                    generateMapElement(all, name, description, dictionaryType, isRequired(true, required));

                }
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                final Class<?> clazz = getType(objectType);
                forceOptional = shouldForceOptional(objectType);

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

                if (ExpressionSupport.REQUIRED != expressionSupport)
                {
                    if (importedTypes.get(objectType) != null)
                    {
                        addImportedTypeRef(getType(importedTypes.get(objectType)), name, description, objectType, all);
                    }
                    else
                    {
                        registerPojoType(objectType, description);
                        addAbstractTypeRef(name, description, objectType, all);
                    }

                    List<MetadataType> subTypes = subTypesMapping.getSubTypes(objectType);
                    if (!subTypes.isEmpty())
                    {
                        registerPojoSubtypes(objectType, subTypes);
                    }
                }
                else
                {
                    //We need to register the type, just in case people want to use it as global elements
                    registerPojoType(objectType, description);
                }
            }

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                extensionType.getAttributeOrAttributeGroup().add(createAttribute(name, description, metadataType, defaultValue, isRequired(forceOptional, required), expressionSupport));
            }

            private boolean shouldForceOptional(MetadataType type)
            {
                return !required ||
                       !subTypesMapping.getSubTypes(type).isEmpty() ||
                       (isInstantiable(getType(type)) && ExpressionSupport.REQUIRED != expressionSupport);
            }

            private boolean isRequired(boolean forceOptional, boolean required)
            {
                return !forceOptional && required;
            }
        };
    }

    private boolean shouldGenerateDataTypeChildElements(MetadataType metadataType, ExpressionSupport expressionSupport)
    {
        boolean isExpressionRequired = ExpressionSupport.REQUIRED == expressionSupport;
        boolean isPojo = metadataType instanceof ObjectType;
        Class<?> clazz = getType(metadataType);
        boolean isPrimitive = clazz.isPrimitive() || ClassUtils.isPrimitiveWrapper(clazz);

        return !isExpressionRequired &&
               (isPrimitive || !subTypesMapping.getSubTypes(metadataType).isEmpty() ||
                (isPojo && isInstantiableWithParameters(clazz)) || (!isPojo && isInstantiable(clazz)));
    }

    private void addImportedTypeRef(Class<?> extensionType, String name, String description, MetadataType metadataType, ExplicitGroup all)
    {
        TopLevelElement objectElement = createTopLevelElement(hyphenize(name), ZERO, "1");
        objectElement.setComplexType(new LocalComplexType());
        objectElement.setAnnotation(createDocAnnotation(description));

        ExplicitGroup sequence = new ExplicitGroup();
        sequence.setMinOccurs(ONE);
        sequence.setMaxOccurs("1");

        XmlModelProperty baseTypeXml = registerExtensionImport(extensionType);

        QName qName = new QName(baseTypeXml.getNamespaceUri(), getTopLevelAbstractTypeName(metadataType), baseTypeXml.getNamespace());
        sequence.getParticle().add(objectFactory.createElement(createRefElement(qName, false)));

        objectElement.getComplexType().setSequence(sequence);

        all.getParticle().add(objectFactory.createElement(objectElement));
    }

    private XmlModelProperty registerExtensionImport(Class<?> extensionType)
    {
        XmlModelProperty importedExtensionXml = createXmlModelProperty(getAnnotation(extensionType, Xml.class),
                                                                       getAnnotation(extensionType, Extension.class).name(), "");

        Import schemaImport = new Import();
        schemaImport.setNamespace(importedExtensionXml.getNamespaceUri());
        schemaImport.setSchemaLocation(importedExtensionXml.getSchemaLocation());
        if (!schema.getIncludeOrImportOrRedefine().contains(schemaImport))
        {
            schema.getIncludeOrImportOrRedefine().add(schemaImport);
        }
        return importedExtensionXml;
    }

    private void registerPojoSubtypes(MetadataType baseType, List<MetadataType> subTypes)
    {
        if (importedTypes.get(baseType) != null)
        {
            registerExtensionImport(getType(importedTypes.get(baseType)));
        }
        else
        {
            registerPojoType(baseType, EMPTY);
        }

        subTypes.forEach(subtype -> registerPojoType(subtype, baseType, EMPTY));
    }

    private void addAbstractTypeRef(String parameterName, String description, MetadataType metadataType, ExplicitGroup all)
    {
        TopLevelElement objectElement = createTopLevelElement(hyphenize(parameterName), ZERO, "1");
        objectElement.setAnnotation(createDocAnnotation(description));
        objectElement.setComplexType(createComplexTypeWithAbstractElementRef(metadataType));

        all.getParticle().add(objectFactory.createElement(objectElement));
    }

    private LocalComplexType createComplexTypeWithAbstractElementRef(MetadataType metadataType)
    {
        ExplicitGroup sequence = new ExplicitGroup();
        sequence.setMinOccurs(ONE);
        sequence.setMaxOccurs("1");

        sequence.getParticle().add(objectFactory.createElement(createRefToLocalElement(metadataType)));

        LocalComplexType complexType = new LocalComplexType();
        complexType.setSequence(sequence);

        return complexType;
    }

    private TopLevelElement createRefToLocalElement(MetadataType metadataType)
    {
        registerPojoType(metadataType, EMPTY);
        QName qName = new QName(xmlProperties.getNamespaceUri(), getTopLevelAbstractTypeName(metadataType), xmlProperties.getNamespace());
        return createRefElement(qName, false);
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

    Schema getSchema()
    {
        return schema;
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
