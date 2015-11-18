/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.extension.api.introspection.DataQualifier.LIST;
import static org.mule.extension.api.introspection.DataQualifier.OPERATION;
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.module.extension.internal.capability.xml.schema.PoolingSupport.REQUIRED;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_DESCRIPTION_CONFIG;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_CONFIG;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.GROUP_SUFFIX;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_EXTENSION_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_SCHEMA_LOCATION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_POOLING_PROFILE_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_SCHEMA_LOCATION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.OPERATION_SUBSTITUTION_GROUP_SUFFIX;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.SPRING_FRAMEWORK_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.SPRING_FRAMEWORK_SCHEMA_LOCATION;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.XML_NAMESPACE;
import static org.mule.module.extension.internal.introspection.ImplicitObjectUtils.getFirstImplicit;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getAlias;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getFieldDataType;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.isIgnored;
import static org.mule.module.extension.internal.util.IntrospectionUtils.isRequired;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getConnectedOperations;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDynamicParameters;
import static org.mule.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.module.extension.internal.util.NameUtils.hyphenize;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.annotation.api.Extensible;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.DataQualifier;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.module.extension.internal.capability.xml.schema.model.Annotation;
import org.mule.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.module.extension.internal.capability.xml.schema.model.ComplexType;
import org.mule.module.extension.internal.capability.xml.schema.model.Documentation;
import org.mule.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.module.extension.internal.capability.xml.schema.model.FormChoice;
import org.mule.module.extension.internal.capability.xml.schema.model.GroupRef;
import org.mule.module.extension.internal.capability.xml.schema.model.Import;
import org.mule.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.module.extension.internal.capability.xml.schema.model.LocalSimpleType;
import org.mule.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.module.extension.internal.capability.xml.schema.model.NoFixedFacet;
import org.mule.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.module.extension.internal.capability.xml.schema.model.Restriction;
import org.mule.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.module.extension.internal.capability.xml.schema.model.SchemaTypeConversion;
import org.mule.module.extension.internal.capability.xml.schema.model.SimpleContent;
import org.mule.module.extension.internal.capability.xml.schema.model.SimpleExtensionType;
import org.mule.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.module.extension.internal.capability.xml.schema.model.TopLevelSimpleType;
import org.mule.module.extension.internal.capability.xml.schema.model.Union;
import org.mule.module.extension.internal.introspection.AbstractDataQualifierVisitor;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.module.extension.internal.util.NameUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.StringUtils;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Builder class to generate a XSD schema that describes a
 * {@link ExtensionModel}
 *
 * @since 3.7.0
 */
public final class SchemaBuilder
{

    private static final String UNBOUNDED = "unbounded";

    private final Set<DataType> registeredEnums = new HashSet<>();
    private final Map<DataType, ComplexTypeHolder> registeredComplexTypesHolders = new HashMap<>();
    private final Map<String, NamedGroup> substitutionGroups = new HashMap<>();
    private final ObjectFactory objectFactory = new ObjectFactory();

    private Schema schema;


    public static SchemaBuilder newSchema(String targetNamespace)
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

        return builder;
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

    public SchemaBuilder registerConnectionProviderElement(ConnectionProviderModel providerModel)
    {
        Element providerElement = new TopLevelElement();
        providerElement.setName(providerModel.getName());
        providerElement.setSubstitutionGroup(MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT);

        LocalComplexType complexType = new LocalComplexType();
        providerElement.setComplexType(complexType);

        ExtensionType providerType = new ExtensionType();
        providerType.setBase(MULE_EXTENSION_CONNECTION_PROVIDER_TYPE);

        ComplexContent complexContent = new ComplexContent();
        complexContent.setExtension(providerType);
        complexType.setComplexContent(complexContent);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(providerElement);

        final ExplicitGroup choice = new ExplicitGroup();
        choice.setMinOccurs(ZERO);
        choice.setMaxOccurs(UNBOUNDED);

        addConnectionProviderPoolingProfile(choice, providerModel);
        registerParameters(providerType, choice, providerModel.getParameterModels());
        return this;
    }

    private void addConnectionProviderPoolingProfile(ExplicitGroup choice, ConnectionProviderModel providerModel)
    {
        PoolingSupport poolingSupport = SchemaBuilderUtils.getPoolingDefinition(providerModel);
        if (poolingSupport == PoolingSupport.NOT_SUPPORTED)
        {
            return;
        }

        TopLevelElement objectElement = new TopLevelElement();

        objectElement.setMinOccurs(poolingSupport == REQUIRED ? ONE : ZERO);
        objectElement.setMaxOccurs("1");
        objectElement.setRef(MULE_POOLING_PROFILE_TYPE);

        choice.getParticle().add(objectFactory.createElement(objectElement));
    }

    public SchemaBuilder registerConfigElement(final ConfigurationModel configurationModel)
    {
        ExtensionType config = registerExtension(configurationModel.getName());
        config.getAttributeOrAttributeGroup().add(createNameAttribute());

        final ExplicitGroup choice = new ExplicitGroup();
        choice.setMinOccurs(ZERO);
        choice.setMaxOccurs(UNBOUNDED);

        addConnectionProviderElement(choice, configurationModel);
        addDynamicConfigPolicyElement(choice, configurationModel);
        registerParameters(config, choice, configurationModel.getParameterModels());
        config.setAnnotation(createDocAnnotation(configurationModel.getDescription()));

        return this;
    }

    private Attribute createNameAttribute()
    {
        return createAttribute(SchemaConstants.ATTRIBUTE_NAME_NAME, DataType.of(String.class), true, NOT_SUPPORTED);
    }

    public SchemaBuilder registerOperation(OperationModel operationModel)
    {
        String typeName = StringUtils.capitalize(operationModel.getName()) + SchemaConstants.TYPE_SUFFIX;
        registerProcessorElement(operationModel, typeName);
        registerOperationType(typeName, operationModel);

        return this;
    }

    private void addConnectionProviderElement(ExplicitGroup all, ConfigurationModel configurationModel)
    {
        ExtensionModel extensionModel = configurationModel.getExtensionModel();
        if (!getConnectedOperations(extensionModel).isEmpty())
        {
            TopLevelElement objectElement = new TopLevelElement();

            objectElement.setMinOccurs(getFirstImplicit(extensionModel.getConnectionProviders()) != null
                                       ? ZERO
                                       : ONE);

            objectElement.setMaxOccurs("1");
            objectElement.setRef(MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT);

            all.getParticle().add(objectFactory.createElement(objectElement));
        }
    }

    private void addDynamicConfigPolicyElement(ExplicitGroup all, ConfigurationModel configurationModel)
    {
        if (!getDynamicParameters(configurationModel).isEmpty())
        {
            TopLevelElement objectElement = new TopLevelElement();
            objectElement.setMinOccurs(ZERO);
            objectElement.setMaxOccurs("1");
            objectElement.setRef(MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT);

            all.getParticle().add(objectFactory.createElement(objectElement));
        }
    }

    private void registerParameters(ExtensionType type, ExplicitGroup choice, Collection<ParameterModel> parameterModels)
    {
        for (final ParameterModel parameterModel : parameterModels)
        {
            parameterModel.getType().getQualifier().accept(new AbstractDataQualifierVisitor()
            {

                private boolean forceOptional = false;

                @Override
                public void onList()
                {
                    forceOptional = true;
                    defaultOperation();
                    generateCollectionElement(choice, parameterModel, true);
                }

                @Override
                public void onPojo()
                {
                    forceOptional = false;
                    defaultOperation();
                    registerComplexTypeChildElement(choice,
                                                    parameterModel.getName(),
                                                    parameterModel.getDescription(),
                                                    parameterModel.getType(),
                                                    isRequired(parameterModel, forceOptional));
                }

                @Override
                protected void defaultOperation()
                {
                    type.getAttributeOrAttributeGroup().add(createAttribute(parameterModel, isRequired(parameterModel, forceOptional)));
                }
            });
        }

        if (!choice.getParticle().isEmpty())
        {
            type.setChoice(choice);
        }
    }

    /**
     * Registers a pojo type creating a base complex type and a substitutable
     * top level type while assigning it a name. This method will not register
     * the same type twice even if requested to
     *
     * @param type        a {@link DataType} referencing a pojo type
     * @param description the type's description
     * @return the reference name of the complexType
     */
    private String registerPojoType(DataType type, String description)
    {
        ComplexTypeHolder alreadyRegisteredType = registeredComplexTypesHolders.get(type);
        if (alreadyRegisteredType != null)
        {
            return alreadyRegisteredType.getComplexType().getName();
        }

        registerBasePojoType(type, description);
        registerPojoGlobalElement(type, description);

        return getBaseTypeName(type);
    }

    private String getBaseTypeName(DataType type)
    {
        return type.getName();
    }

    private TopLevelComplexType registerBasePojoType(DataType type, String description)
    {
        final TopLevelComplexType complexType = new TopLevelComplexType();
        registeredComplexTypesHolders.put(type, new ComplexTypeHolder(complexType, type));

        complexType.setName(type.getName());
        complexType.setAnnotation(createDocAnnotation(description));

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);

        final ExtensionType extension = new ExtensionType();
        extension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(extension);

        final ExplicitGroup all = new ExplicitGroup();
        extension.setSequence(all);

        for (Field field : getParameterFields(type.getRawType()))
        {
            if (isIgnored(field))
            {
                continue;
            }

            final String name = getAlias(field);
            final DataType fieldType = getFieldDataType(field);
            final boolean required = isRequired(field);
            final ExpressionSupport expressionSupport = getExpressionSupport(field);

            fieldType.getQualifier().accept(new AbstractDataQualifierVisitor()
            {

                @Override
                public void onList()
                {
                    generateCollectionElement(all, name, EMPTY, fieldType, required);
                }

                @Override
                public void onPojo()
                {
                    registerComplexTypeChildElement(all, name, EMPTY, fieldType, false);
                }

                @Override
                protected void defaultOperation()
                {
                    Attribute attribute = createAttribute(name, fieldType, required, expressionSupport);
                    extension.getAttributeOrAttributeGroup().add(attribute);
                }
            });
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
        return complexType;
    }

    public SchemaBuilder registerEnums()
    {
        for (DataType enumToBeRegistered : registeredEnums)
        {
            registerEnum(schema, enumToBeRegistered);
        }

        return this;
    }

    private void registerEnum(Schema schema, DataType enumType)
    {
        TopLevelSimpleType enumSimpleType = new TopLevelSimpleType();
        enumSimpleType.setName(enumType.getName() + SchemaConstants.ENUM_TYPE_SUFFIX);

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

    private LocalSimpleType createEnumSimpleType(DataType enumType)
    {
        LocalSimpleType enumValues = new LocalSimpleType();
        Restriction restriction = new Restriction();
        enumValues.setRestriction(restriction);
        restriction.setBase(SchemaConstants.STRING);


        Class<? extends Enum> enumClass = (Class<? extends Enum>) enumType.getRawType();

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
                                                 DataType type,
                                                 boolean required)
    {
        name = hyphenize(name);

        // this top level element is for declaring the object inside a config or operation
        TopLevelElement objectElement = new TopLevelElement();
        objectElement.setName(name);
        objectElement.setMinOccurs(required ? ONE : ZERO);
        objectElement.setMaxOccurs("1");
        objectElement.setComplexType(newLocalComplexTypeWithBase(type, description));
        objectElement.setAnnotation(createDocAnnotation(description));

        all.getParticle().add(objectFactory.createElement(objectElement));
    }

    private void registerPojoGlobalElement(DataType type, String description)
    {
        TopLevelElement objectElement = new TopLevelElement();
        objectElement.setName(getTopLevelTypeName(type));

        LocalComplexType complexContent = newLocalComplexTypeWithBase(type, description);
        complexContent.getComplexContent().getExtension().getAttributeOrAttributeGroup().add(createNameAttribute());
        objectElement.setComplexType(complexContent);

        objectElement.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);
        objectElement.setAnnotation(createDocAnnotation(description));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(objectElement);
    }

    private LocalComplexType newLocalComplexTypeWithBase(DataType type, String description)
    {
        LocalComplexType objectComplexType = new LocalComplexType();
        objectComplexType.setComplexContent(new ComplexContent());
        objectComplexType.getComplexContent().setExtension(new ExtensionType());
        objectComplexType.getComplexContent().getExtension().setBase(
                new QName(schema.getTargetNamespace(), registerPojoType(type, description))
        ); // base to the pojo type

        return objectComplexType;
    }

    private ExtensionType registerExtension(String name)
    {
        LocalComplexType complexType = new LocalComplexType();

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);
        extension.setComplexType(complexType);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);

        return complexContentExtension;
    }

    private Attribute createAttribute(ParameterModel parameterModel, boolean required)
    {
        return createAttribute(parameterModel.getName(), parameterModel.getDescription(), parameterModel.getType(), required, parameterModel.getExpressionSupport());
    }

    private Attribute createAttribute(String name, DataType type, boolean required, ExpressionSupport expressionSupport)
    {
        return createAttribute(name, EMPTY, type, required, expressionSupport);
    }


    private Attribute createAttribute(final String name, String description, final DataType type, boolean required, final ExpressionSupport expressionSupport)
    {
        final Attribute attribute = new Attribute();
        attribute.setUse(required ? SchemaConstants.USE_REQUIRED : SchemaConstants.USE_OPTIONAL);
        attribute.setAnnotation(createDocAnnotation(description));

        type.getQualifier().accept(new AbstractDataQualifierVisitor()
        {

            @Override
            public void onEnum()
            {
                attribute.setName(name);
                attribute.setType(new QName(schema.getTargetNamespace(), type.getName() + SchemaConstants.ENUM_TYPE_SUFFIX));
                registeredEnums.add(type);
            }

            @Override
            protected void defaultOperation()
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
        generateCollectionElement(all, parameterModel.getName(), parameterModel.getDescription(), parameterModel.getType(), required);
    }

    private void generateCollectionElement(ExplicitGroup all, String name, String description, DataType type, boolean required)
    {
        name = hyphenize(name);

        BigInteger minOccurs = required ? ONE : ZERO;
        String collectionName = hyphenize(NameUtils.singularize(name));
        LocalComplexType collectionComplexType = generateCollectionComplexType(collectionName, description, type);

        TopLevelElement collectionElement = new TopLevelElement();
        collectionElement.setName(name);
        collectionElement.setMinOccurs(minOccurs);
        collectionElement.setMaxOccurs("1");
        collectionElement.setAnnotation(createDocAnnotation(description));
        all.getParticle().add(objectFactory.createElement(collectionElement));

        collectionElement.setComplexType(collectionComplexType);
    }

    private LocalComplexType generateCollectionComplexType(String name, final String description, final DataType type)
    {
        final LocalComplexType collectionComplexType = new LocalComplexType();
        final ExplicitGroup sequence = new ExplicitGroup();
        collectionComplexType.setSequence(sequence);

        final TopLevelElement collectionItemElement = new TopLevelElement();
        collectionItemElement.setName(name);
        collectionItemElement.setMinOccurs(ZERO);
        collectionItemElement.setMaxOccurs(SchemaConstants.UNBOUNDED);

        final DataType genericType = getFirstGenericType(type);
        genericType.getQualifier().accept(new AbstractDataQualifierVisitor()
        {

            @Override
            public void onPojo()
            {
                collectionItemElement.setComplexType(newLocalComplexTypeWithBase(genericType, description));
            }

            @Override
            protected void defaultOperation()
            {
                collectionItemElement.setComplexType(generateComplexValueType(genericType));
            }
        });

        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

        return collectionComplexType;
    }

    private LocalComplexType generateComplexValueType(DataType type)
    {
        LocalComplexType complexType = new LocalComplexType();
        SimpleContent simpleContent = new SimpleContent();
        complexType.setSimpleContent(simpleContent);
        SimpleExtensionType simpleContentExtension = new SimpleExtensionType();
        QName extensionBase = SchemaTypeConversion.convertType(type, NOT_SUPPORTED);
        simpleContentExtension.setBase(extensionBase);
        simpleContent.setExtension(simpleContentExtension);

        Attribute valueAttribute = createAttribute(ATTRIBUTE_NAME_VALUE, type, true, SUPPORTED);
        simpleContentExtension.getAttributeOrAttributeGroup().add(valueAttribute);

        return complexType;
    }

    private DataType getFirstGenericType(DataType type)
    {
        return ArrayUtils.isEmpty(type.getGenericTypes()) ? type : type.getGenericTypes()[0];
    }

    private void registerProcessorElement(OperationModel operationModel, String typeName)
    {
        Element element = new TopLevelElement();
        element.setName(getOperationName(operationModel));
        element.setType(new QName(schema.getTargetNamespace(), typeName));
        element.setAnnotation(createDocAnnotation(operationModel.getDescription()));
        element.setSubstitutionGroup(getOperationSubstitutionGroup(operationModel));
        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private QName getOperationSubstitutionGroup(OperationModel operationModel)
    {
        QName substitutionGroup = MULE_ABSTRACT_MESSAGE_PROCESSOR;
        ExtendingOperationModelProperty extendingOperationModelProperty = operationModel.getModelProperty(ExtendingOperationModelProperty.class.getName());
        if (extendingOperationModelProperty != null)
        {
            substitutionGroup = getSubstitutionGroup(extendingOperationModelProperty.getType());
        }

        return substitutionGroup;
    }

    private QName getSubstitutionGroup(Class<?> type)
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

    private String getOperationName(OperationModel operationModel)
    {
        return hyphenize(operationModel.getName());
    }

    private String getGroupName(String name)
    {
        return name + GROUP_SUFFIX;
    }

    private void registerOperationType(String name, OperationModel operationModel)
    {
        final QName base = MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE;
        TopLevelComplexType complexType = new TopLevelComplexType();
        complexType.setName(name);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        final ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(base);
        complexContent.setExtension(complexContentExtension);

        Attribute configAttr = createAttribute(ATTRIBUTE_NAME_CONFIG, ATTRIBUTE_DESCRIPTION_CONFIG, true, SUBSTITUTABLE_NAME);
        complexContentExtension.getAttributeOrAttributeGroup().add(configAttr);

        final ExplicitGroup all = new ExplicitGroup();
        complexContentExtension.setSequence(all);

        for (final ParameterModel parameterModel : operationModel.getParameterModels())
        {
            DataType parameterType = parameterModel.getType();
            DataQualifier parameterQualifier = parameterType.getQualifier();

            if (isOperation(parameterType))
            {
                String maxOccurs = parameterQualifier == DataQualifier.LIST ? UNBOUNDED : "1";
                generateNestedProcessorElement(all, parameterModel, maxOccurs);
            }
            else
            {
                parameterQualifier.accept(new AbstractDataQualifierVisitor()
                {
                    private boolean forceOptional = false;

                    @Override
                    public void onList()
                    {
                        forceOptional = true;
                        defaultOperation();
                        generateCollectionElement(all, parameterModel, true);
                    }

                    @Override
                    public void onPojo()
                    {
                        forceOptional = true;
                        defaultOperation();
                        registerComplexTypeChildElement(all,
                                                        parameterModel.getName(),
                                                        parameterModel.getDescription(),
                                                        parameterModel.getType(),
                                                        false);
                    }

                    @Override
                    protected void defaultOperation()
                    {
                        complexContentExtension.getAttributeOrAttributeGroup().add(createAttribute(parameterModel, isRequired(parameterModel, forceOptional)));
                    }
                });
            }
        }

        if (all.getParticle().size() == 0)
        {
            complexContentExtension.setSequence(null);
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
    }

    private boolean isOperation(DataType type)
    {
        DataType[] genericTypes = type.getGenericTypes();
        DataQualifier qualifier = type.getQualifier();

        return OPERATION.equals(qualifier) ||
               (LIST.equals(qualifier) &&
                !ArrayUtils.isEmpty(genericTypes) &&
                OPERATION.equals(genericTypes[0].getQualifier()));
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
        QName ref = MULE_MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT_TYPE;
        TypeRestrictionModelProperty restrictionCapability = parameterModel.getModelProperty(TypeRestrictionModelProperty.KEY);
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

    private Annotation createDocAnnotation(String content)
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

    private class ComplexTypeHolder
    {

        private ComplexType complexType;
        private DataType type;

        public ComplexTypeHolder(ComplexType complexType, DataType type)
        {
            this.complexType = complexType;
            this.type = type;
        }

        public ComplexType getComplexType()
        {
            return complexType;
        }

        public DataType getType()
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
