/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.lang.String.format;
import static org.mule.metadata.java.utils.JavaTypeUtils.getGenericTypeAt;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.LITERAL;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.hyphenize;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.pluralize;
import static org.mule.runtime.config.spring.dsl.api.xml.NameUtils.singularize;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.config.dsl.object.ValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.FixedTypeParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TopLevelParameterObjectFactory;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NestedProcessorListValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NestedProcessorValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Base class for parsers delegates which generate {@link ComponentBuildingDefinition}
 * instances for the specific components types (configs, operations, providers, etc) which
 * constitute an extension.
 * <p>
 * It works under the premise that this parser will be used to generate an instance of
 * {@link AbstractExtensionObjectFactory}, using the {@link AbstractExtensionObjectFactory#setParameters(Map)}
 * to provide a map of values containing the parsed attributes.
 * <p>
 * It also gives the opportunity to generate extra {@link ComponentBuildingDefinition} which can
 * also be registered by invoking {@link #addDefinition(ComponentBuildingDefinition)}
 *
 * @since 4.0
 */
public abstract class ExtensionDefinitionParser
{

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String CALENDAR_FORMAT = "yyyy-MM-dd'T'hh:mm:ssX";
    static final String CHILD_ELEMENT_KEY_PREFIX = "<<";
    static final String CHILD_ELEMENT_KEY_SUFFIX = ">>";

    private final TemplateParser parser = TemplateParser.createMuleStyleParser();
    private final ConversionService conversionService = new DefaultConversionService();
    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private final Map<String, AttributeDefinition.Builder> parameters = new HashMap<>();
    private final Builder baseDefinitionBuilder;
    private final List<ComponentBuildingDefinition> parsedDefinitions = new ArrayList<>();
    private final ObjectParsingDelegate defaultObjectParsingDelegate = new ValueResolverParsingDelegate();
    private final List<ObjectParsingDelegate> objectParsingDelegates = ImmutableList.of(
            new FixedTypeParsingDelegate(PoolingProfile.class),
            new FixedTypeParsingDelegate(RetryPolicyTemplate.class),
            new FixedTypeParsingDelegate(TlsContextFactory.class),
            new FixedTypeParsingDelegate(ThreadingProfile.class));

    /**
     * Creates a new instance
     *
     * @param baseDefinitionBuilder a {@link Builder} used as a prototype to generate new defitintions
     */
    protected ExtensionDefinitionParser(Builder baseDefinitionBuilder)
    {
        this.baseDefinitionBuilder = baseDefinitionBuilder;
    }

    /**
     * Creates a list of {@link ComponentBuildingDefinition} built on copies of
     * {@link #baseDefinitionBuilder}. It also sets the parsed parsed parameters
     * on the backing {@link AbstractExtensionObjectFactory}
     *
     * @return a list with the generated {@link ComponentBuildingDefinition}
     * @throws ConfigurationException if a parsing error occurs
     */
    public final List<ComponentBuildingDefinition> parse() throws ConfigurationException
    {
        final Builder builder = baseDefinitionBuilder.copy();
        doParse(builder);

        AttributeDefinition parametersDefinition = fromFixedValue(new HashMap<>()).build();
        if (!parameters.isEmpty())
        {
            KeyAttributeDefinitionPair[] attributeDefinitions = parameters.entrySet().stream()
                    .map(entry -> newBuilder().withAttributeDefinition(entry.getValue().build()).withKey(entry.getKey()).build())
                    .toArray(KeyAttributeDefinitionPair[]::new);
            parametersDefinition = fromMultipleDefinitions(attributeDefinitions).build();
        }

        builder.withSetterParameterDefinition("parameters", parametersDefinition);

        addDefinition(builder.build());
        return parsedDefinitions;
    }

    /**
     * Implementations place their custom parsing logic here.
     *
     * @param definitionBuilder the {@link Builder} on which implementation are to define their stuff
     * @throws ConfigurationException if a parsing error occurs
     */
    protected abstract void doParse(Builder definitionBuilder) throws ConfigurationException;

    /**
     * Parsers the given {@code parameters} and generates matching definitions
     *
     * @param parameters a list of {@link ParameterModel}
     */
    protected void parseParameters(List<ParameterModel> parameters)
    {
        parameters.forEach(parameter -> {
            parameter.getType().accept(new MetadataTypeVisitor()
            {
                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    parseAttributeParameter(parameter);
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    if (isExpressionFunction(objectType))
                    {
                        defaultVisit(objectType);
                        return;
                    }
                    else if (isNestedProcessor(objectType))
                    {
                        parseNestedProcessor(parameter);
                    }
                    else
                    {
                        parseObjectParameter(parameter);
                    }
                }

                @Override
                public void visitDictionary(DictionaryType dictionaryType)
                {
                    parseMapParameters(parameter, dictionaryType);
                }

                @Override
                public void visitArrayType(ArrayType arrayType)
                {
                    if (isNestedProcessor(arrayType.getType()))
                    {
                        parseNestedProcessorList(parameter);
                    }
                    else
                    {
                        parseCollectionParameter(parameter, arrayType);
                    }
                }

                private boolean isNestedProcessor(MetadataType type)
                {
                    return NestedProcessor.class.isAssignableFrom(getType(type));
                }
            });
        });
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents a {@link DictionaryType}
     *
     * @param parameter      a {@link ParameterModel}
     * @param dictionaryType a {@link DictionaryType}
     */
    protected void parseMapParameters(ParameterModel parameter, DictionaryType dictionaryType)
    {
        parseMapParameters(getKey(parameter), parameter.getName(), dictionaryType, parameter.getDefaultValue(), parameter.getExpressionSupport(), parameter.isRequired());
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents a {@link DictionaryType}
     *
     * @param key               the key that the parsed value should have on the parsed parameter's map
     * @param name              the parameter's name
     * @param dictionaryType    the parameter's {@link DictionaryType}
     * @param defaultValue      the parameter's default value
     * @param expressionSupport the parameter's {@link ExpressionSupport}
     * @param required          whether the parameter is required
     */
    protected void parseMapParameters(String key, String name, DictionaryType dictionaryType, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        parseAttributeParameter(key, name, dictionaryType, defaultValue, expressionSupport, required);

        Class<? extends Map> mapType = getType(dictionaryType);
        if (ConcurrentMap.class.equals(mapType))
        {
            mapType = ConcurrentHashMap.class;
        }
        else if (Map.class.equals(mapType))
        {
            mapType = LinkedHashMap.class;
        }

        final MetadataType keyType = dictionaryType.getKeyType();
        final MetadataType valueType = dictionaryType.getValueType();
        final Class<?> keyClass = getType(keyType);
        final Class<?> valueClass = getType(valueType);
        final String parameterName = name;
        final String mapElementName = hyphenize(pluralize(parameterName));

        addParameter(getChildKey(key), fromChildMapConfiguration(keyClass, valueClass)
                .withWrapperIdentifier(mapElementName)
                .withDefaultValue(defaultValue));

        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(mapElementName)
                              .withTypeDefinition(fromType(mapType))
                              .build());

        String entryElementName = hyphenize(singularize(parameterName));

        valueType.accept(new MetadataTypeVisitor()
        {

            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                addDefinition(baseDefinitionBuilder.copy()
                                      .withIdentifier(entryElementName)
                                      .withTypeDefinition(fromMapEntryType(keyClass, valueClass))
                                      .withKeyTypeConverter(value -> resolverOf(parameterName, keyType, value, null, expressionSupport, true))
                                      .withTypeConverter(value -> resolverOf(parameterName, valueType, value, null, expressionSupport, true))
                                      .build());
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                defaultVisit(arrayType);
                final String itemElementName = entryElementName + "-item";
                arrayType.getType().accept(new BasicTypeMetadataVisitor()
                {
                    @Override
                    protected void visitBasicType(MetadataType metadataType)
                    {
                        addDefinition(baseDefinitionBuilder.copy()
                                              .withIdentifier(itemElementName)
                                              .withTypeDefinition(fromType(getType(metadataType)))
                                              .withTypeConverter(value -> resolverOf(parameterName, metadataType, value, getDefaultValue(metadataType), getExpressionSupport(metadataType), false))
                                              .build());
                    }

                    @Override
                    protected void defaultVisit(MetadataType metadataType)
                    {
                        addDefinition(baseDefinitionBuilder.copy()
                                              .withIdentifier(itemElementName)
                                              .withTypeDefinition(fromType(ValueResolver.class))
                                              .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                                              .withConstructorParameterDefinition(fromFixedValue(arrayType.getType()).build())
                                              .build());
                    }
                });
            }
        });
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents an {@link ArrayType}
     *
     * @param parameter a {@link ParameterModel}
     * @param arrayType an {@link ArrayType}
     */
    protected void parseCollectionParameter(ParameterModel parameter, ArrayType arrayType)
    {
        parseCollectionParameter(getKey(parameter), parameter.getName(), arrayType, parameter.getDefaultValue(), parameter.getExpressionSupport(), parameter.isRequired());
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents an {@link ArrayType}
     *
     * @param key               the key that the parsed value should have on the parsed parameter's map
     * @param name              the parameter's name
     * @param arrayType         the parameter's {@link ArrayType}
     * @param defaultValue      the parameter's default value
     * @param expressionSupport the parameter's {@link ExpressionSupport}
     * @param required          whether the parameter is required
     */
    protected void parseCollectionParameter(String key, String name, ArrayType arrayType, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        parseAttributeParameter(key, name, arrayType, defaultValue, expressionSupport, required);

        Class<? extends Iterable> collectionType = getType(arrayType);

        if (Set.class.equals(collectionType))
        {
            collectionType = HashSet.class;
        }
        else if (Collection.class.equals(collectionType) || Iterable.class.equals(collectionType) || collectionType == null)
        {
            collectionType = List.class;
        }

        final String collectionElementName = hyphenize(name);
        addParameter(getChildKey(key), fromChildConfiguration(collectionType).withWrapperIdentifier(collectionElementName));
        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(collectionElementName)
                              .withTypeDefinition(fromType(collectionType))
                              .build());

        String itemIdentifier;
        MetadataType itemType = arrayType.getType();
        if (itemType instanceof ObjectType)
        {
            itemIdentifier = getTopLevelTypeName(itemType);
        }
        else
        {
            itemIdentifier = hyphenize(singularize(name));
        }

        Builder itemDefinitionBuilder = baseDefinitionBuilder.copy().withIdentifier(itemIdentifier);

        arrayType.getType().accept(new BasicTypeMetadataVisitor()
        {
            @Override
            protected void visitBasicType(MetadataType metadataType)
            {
                itemDefinitionBuilder.withTypeDefinition(fromType(getType(metadataType)))
                        .withTypeConverter(value -> resolverOf(name,
                                                               metadataType,
                                                               value,
                                                               getDefaultValue(metadataType).orElse(null),
                                                               getExpressionSupport(metadataType),
                                                               false));
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                itemDefinitionBuilder.withTypeDefinition(fromType(ValueResolver.class))
                        .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                        .withConstructorParameterDefinition(fromFixedValue(objectType).build());
            }
        });

        addDefinition(itemDefinitionBuilder.build());
    }

    private ValueResolver<?> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        if (value instanceof ValueResolver)
        {
            return (ValueResolver<?>) value;
        }

        ValueResolver resolver = null;
        if (expressionSupport == LITERAL)
        {
            return new StaticValueResolver<>(value);
        }

        if (isExpressionFunction(expectedType) && value != null)
        {
            resolver = new ExpressionFunctionValueResolver<>((String) value, getGenericTypeAt((ObjectType) expectedType, 1, typeLoader).get());
        }

        final Class<Object> expectedClass = getType(expectedType);
        if (resolver == null)
        {
            if (isExpression(value, parser))
            {
                resolver = new TypeSafeExpressionValueResolver((String) value, expectedClass);
            }
        }

        if (resolver == null && value != null)
        {
            final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
            expectedType.accept(new BasicTypeMetadataVisitor()
            {
                @Override
                protected void visitBasicType(MetadataType metadataType)
                {
                    if (conversionService.canConvert(value.getClass(), expectedClass))
                    {
                        resolverValueHolder.set(new StaticValueResolver(conversionService.convert(value, expectedClass)));
                    }
                    else
                    {
                        defaultVisit(metadataType);
                    }
                }

                @Override
                public void visitDateTime(DateTimeType dateTimeType)
                {
                    resolverValueHolder.set(parseCalendar(value, dateTimeType, defaultValue));
                }

                @Override
                public void visitDate(DateType dateType)
                {
                    resolverValueHolder.set(parseDate(value, dateType, defaultValue));
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    resolverValueHolder.set(new RegistryLookupValueResolver(value.toString()));
                }
            });

            resolver = resolverValueHolder.get();
        }

        if (resolver == null)
        {
            resolver = new StaticValueResolver<>(defaultValue);
        }

        if (resolver.isDynamic() && expressionSupport == NOT_SUPPORTED)
        {
            throw new IllegalArgumentException(format("An expression value was given for parameter '%s' but it doesn't support expressions", parameterName));
        }

        if (!resolver.isDynamic() && expressionSupport == REQUIRED && required)
        {
            throw new IllegalArgumentException(format("A fixed value was given for parameter '%s' but it only supports expressions", parameterName));
        }

        return resolver;
    }

    private boolean isExpression(Object value, TemplateParser parser)
    {
        return value instanceof String && parser.isContainsTemplate((String) value);
    }

    /**
     * Registers a definition for parsing the given {@code parameterModel}
     * as an element attribute
     *
     * @param parameterModel a {@link ParameterModel}
     * @return an {@link AttributeDefinition.Builder}
     */
    protected AttributeDefinition.Builder parseAttributeParameter(ParameterModel parameterModel)
    {
        return parseAttributeParameter(getKey(parameterModel),
                                       parameterModel.getName(),
                                       parameterModel.getType(),
                                       parameterModel.getDefaultValue(),
                                       parameterModel.getExpressionSupport(),
                                       parameterModel.isRequired());
    }

    /**
     * Registers a definition for parsing the given {@code parameterModel}
     * as an element attribute
     *
     * @param key               the key that the parsed value should have on the parsed parameter's map
     * @param name              the parameter's name
     * @param type              the parameter's type
     * @param defaultValue      the parameter's default value
     * @param expressionSupport the parameter's {@link ExpressionSupport}
     * @param required          whether the parameter is required or not
     * @return an {@link AttributeDefinition.Builder}
     */
    protected AttributeDefinition.Builder parseAttributeParameter(String key, String name, MetadataType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        AttributeDefinition.Builder definitionBuilder = fromSimpleParameter(name, value -> resolverOf(name, type, value, defaultValue, expressionSupport, required));
        addParameter(key, definitionBuilder);

        return definitionBuilder;
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents an {@link ObjectType}
     *
     * @param parameterModel a {@link ParameterModel}
     */
    protected void parseObjectParameter(ParameterModel parameterModel)
    {
        parseObjectParameter(getKey(parameterModel),
                             parameterModel.getName(),
                             (ObjectType) parameterModel.getType(),
                             parameterModel.getDefaultValue(),
                             parameterModel.getExpressionSupport(),
                             parameterModel.isRequired());
    }

    /**
     * Registers a definition for a {@link ParameterModel} which represents an {@link ObjectType}
     *
     * @param key               the key that the parsed value should have on the parsed parameter's map
     * @param name              the parameter's name
     * @param type              an {@link ObjectType}
     * @param defaultValue      the parameter's default value
     * @param expressionSupport the parameter's {@link ExpressionSupport}
     * @param required          whether the parameter is required or not
     */
    protected void parseObjectParameter(String key, String name, ObjectType type, Object defaultValue, ExpressionSupport expressionSupport, boolean required)
    {
        parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required);
        ObjectParsingDelegate delegate = objectParsingDelegates.stream()
                .filter(candidate -> candidate.accepts(type))
                .findFirst()
                .orElse(defaultObjectParsingDelegate);

        addParameter(getChildKey(key), delegate.parse(name, type));
    }

    /**
     * Adds the given {@code definition} to the list of definitions
     * that the {@link #parse()} method generates by default
     *
     * @param definition a {@link ComponentBuildingDefinition}
     */
    protected void addDefinition(ComponentBuildingDefinition definition)
    {
        parsedDefinitions.add(definition);
    }

    private void addParameter(String key, AttributeDefinition.Builder definitionBuilder)
    {
        parameters.put(key, definitionBuilder);
    }

    private void parseNestedProcessor(ParameterModel parameterModel)
    {
        final String processorElementName = hyphenize(parameterModel.getName());
        addParameter(getChildKey(parameterModel.getName()), fromChildConfiguration(NestedProcessorValueResolver.class).withWrapperIdentifier(processorElementName));

        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(processorElementName)
                              .withTypeDefinition(fromType(NestedProcessorValueResolver.class))
                              .withConstructorParameterDefinition(fromChildConfiguration(MessageProcessor.class).build())
                              .build());
    }

    private void parseNestedProcessorList(ParameterModel parameterModel)
    {
        final String processorElementName = hyphenize(parameterModel.getName());
        addParameter(getChildKey(parameterModel.getName()), fromChildCollectionConfiguration(NestedProcessorListValueResolver.class).withWrapperIdentifier(processorElementName));

        addDefinition(baseDefinitionBuilder.copy()
                              .withIdentifier(processorElementName)
                              .withTypeDefinition(fromType(NestedProcessorListValueResolver.class))
                              .withConstructorParameterDefinition(fromChildCollectionConfiguration(MessageProcessor.class).build())
                              .build());

    }

    private boolean isExpressionFunction(MetadataType metadataType)
    {
        if (!Function.class.isAssignableFrom(getType(metadataType)))
        {
            return false;
        }

        GenericTypesAnnotation generics = getSingleAnnotation(metadataType, GenericTypesAnnotation.class).orElse(null);
        if (generics == null)
        {
            return false;
        }

        if (generics.getGenericTypes().size() != 2)
        {
            return false;
        }

        final String genericClassName = generics.getGenericTypes().get(0);
        try
        {
            return MuleEvent.class.isAssignableFrom(ClassUtils.getClass(genericClassName));
        }
        catch (ClassNotFoundException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not load class " + genericClassName), e);
        }
    }

    private ValueResolver parseCalendar(Object value, DateTimeType dataType, Object defaultValue)
    {
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dataType));
        }

        Date date = doParseDate(value, CALENDAR_FORMAT, defaultValue);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new StaticValueResolver(calendar);
    }

    private ValueResolver parseDate(Object value, DateType dateType, Object defaultValue)
    {
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dateType));
        }
        else
        {
            return new StaticValueResolver(doParseDate(value, DATE_FORMAT, defaultValue));
        }
    }

    private Date doParseDate(Object value, String parseFormat, Object defaultValue)
    {
        if (value == null)
        {
            if (defaultValue == null)
            {
                return null;
            }

            value = defaultValue;
        }

        if (value instanceof String)
        {
            SimpleDateFormat format = new SimpleDateFormat(parseFormat);
            try
            {
                return format.parse((String) value);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(format("Could not transform value '%s' into a Date using pattern '%s'", value, parseFormat));
            }
        }

        if (value instanceof Date)
        {
            return (Date) value;
        }

        throw new IllegalArgumentException(format("Could not transform value of type '%s' to Date", value != null ? value.getClass().getName() : "null"));
    }


    private String getKey(ParameterModel parameterModel)
    {
        return getMemberName(parameterModel, parameterModel.getName());
    }

    private String getChildKey(String key)
    {
        return String.format("%s%s%s", CHILD_ELEMENT_KEY_PREFIX, key, CHILD_ELEMENT_KEY_SUFFIX);
    }
}

