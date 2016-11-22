/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.object.CharsetValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.FixedTypeParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.MediaTypeValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.ObjectTypeParameterParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TopLevelParameterObjectFactory;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.introspection.describer.FunctionParameterTypeModelProperty;
import org.mule.runtime.module.extension.internal.introspection.describer.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedParameterResolverValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NativeQueryParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NestedProcessorListValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NestedProcessorValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Base class for parsers delegates which generate {@link ComponentBuildingDefinition} instances for the specific components types
 * (configs, operations, providers, etc) which constitute an extension.
 * <p>
 * It works under the premise that this parser will be used to generate an instance of {@link AbstractExtensionObjectFactory},
 * using the {@link AbstractExtensionObjectFactory#setParameters(Map)} to provide a map of values containing the parsed
 * attributes.
 * <p>
 * It also gives the opportunity to generate extra {@link ComponentBuildingDefinition} which can also be registered by invoking
 * {@link #addDefinition(ComponentBuildingDefinition)}
 *
 * @since 4.0
 */
public abstract class ExtensionDefinitionParser {

  static final String CHILD_ELEMENT_KEY_PREFIX = "<<";
  static final String CHILD_ELEMENT_KEY_SUFFIX = ">>";
  protected static final String CONFIG_PROVIDER_ATTRIBUTE_NAME = "configurationProvider";

  protected final ExtensionParsingContext parsingContext;
  protected final List<ObjectParsingDelegate> objectParsingDelegates = ImmutableList
      .of(new FixedTypeParsingDelegate(PoolingProfile.class), new FixedTypeParsingDelegate(RetryPolicyTemplate.class),
          new FixedTypeParsingDelegate(TlsContextFactory.class), new DefaultObjectParsingDelegate());
  protected final DslSyntaxResolver dslResolver;
  protected final Builder baseDefinitionBuilder;
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();
  private final ConversionService conversionService = new DefaultConversionService();
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final Map<String, AttributeDefinition.Builder> parameters = new HashMap<>();
  private final List<ComponentBuildingDefinition> parsedDefinitions = new ArrayList<>();
  private final List<ValueResolverParsingDelegate> valueResolverParsingDelegates =
      ImmutableList.of(new CharsetValueResolverParsingDelegate(), new MediaTypeValueResolverParsingDelegate());
  private final ValueResolverParsingDelegate defaultValueResolverParsingDelegate = new DefaultValueResolverParsingDelegate();
  protected final MuleContext muleContext;

  /**
   * Creates a new instance
   *
   * @param baseDefinitionBuilder a {@link Builder} used as a prototype to generate new defitintions
   * @param dslSyntaxResolver     a {@link DslSyntaxResolver} instance associated with the {@link ExtensionModel} being parsed
   * @param parsingContext        the {@link ExtensionParsingContext} in which {@code this} parser operates
   */
  protected ExtensionDefinitionParser(Builder baseDefinitionBuilder, DslSyntaxResolver dslSyntaxResolver,
                                      ExtensionParsingContext parsingContext, MuleContext muleContext) {
    this.baseDefinitionBuilder = baseDefinitionBuilder;
    this.dslResolver = dslSyntaxResolver;
    this.parsingContext = parsingContext;
    this.muleContext = muleContext;
  }

  /**
   * Creates a list of {@link ComponentBuildingDefinition} built on copies of {@link #baseDefinitionBuilder}. It also sets the
   * parsed parsed parameters on the backing {@link AbstractExtensionObjectFactory}
   *
   * @return a list with the generated {@link ComponentBuildingDefinition}
   * @throws ConfigurationException if a parsing error occurs
   */
  public final List<ComponentBuildingDefinition> parse() throws ConfigurationException {
    final Builder builder = baseDefinitionBuilder.copy();
    doParse(builder);

    AttributeDefinition parametersDefinition = fromFixedValue(new HashMap<>()).build();
    if (!parameters.isEmpty()) {
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
  protected void parseParameters(List<ParameterModel> parameters) {
    parameters.forEach(parameter -> {

      final DslElementSyntax paramDsl = dslResolver.resolve(parameter);
      final boolean isContent = isContent(parameter);

      parameter.getType().accept(new MetadataTypeVisitor() {

        @Override
        protected void defaultVisit(MetadataType metadataType) {
          parseAttributeParameter(parameter);
        }

        @Override
        public void visitString(StringType stringType) {
          if (paramDsl.supportsChildDeclaration()) {
            parseFromTextExpression(parameter, paramDsl, () -> {
              Optional<QueryParameterModelProperty> query = parameter.getModelProperty(QueryParameterModelProperty.class);
              if (query.isPresent()) {
                return value -> new NativeQueryParameterValueResolver((String) value, query.get().getQueryTranslator());
              } else {
                return value -> resolverOf(parameter.getName(), stringType, value, parameter.getDefaultValue(),
                                           parameter.getExpressionSupport(), parameter.isRequired(),
                                           parameter.getModelProperties(), acceptsReferences(parameter));
              }
            });
          } else {
            defaultVisit(stringType);
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          if (isNestedProcessor(objectType)) {
            parseNestedProcessor(parameter);
          } else {
            parseObjectParameter(parameter, paramDsl);
          }
        }

        @Override
        public void visitDictionary(DictionaryType dictionaryType) {
          if (!parseAsContent(dictionaryType)) {
            parseMapParameters(parameter, dictionaryType, paramDsl);
          }
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (isNestedProcessor(arrayType.getType())) {
            parseNestedProcessorList(parameter);
          } else if (!parseAsContent(arrayType)) {
            parseCollectionParameter(parameter, arrayType, paramDsl);
          }
        }

        private boolean parseAsContent(MetadataType type) {
          if (isContent) {
            parseFromTextExpression(parameter, paramDsl,
                                    () -> value -> resolverOf(parameter.getName(), type, value, parameter.getDefaultValue(),
                                                              parameter.getExpressionSupport(), parameter.isRequired(),
                                                              parameter.getModelProperties(), acceptsReferences(parameter)));

            return true;
          }

          return false;
        }

        private boolean isNestedProcessor(MetadataType type) {
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
  protected void parseMapParameters(ParameterModel parameter, DictionaryType dictionaryType, DslElementSyntax paramDsl) {
    parseMapParameters(getKey(parameter), parameter.getName(), dictionaryType, parameter.getDefaultValue(),
                       parameter.getExpressionSupport(), parameter.isRequired(), paramDsl, parameter.getModelProperties());
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
  protected void parseMapParameters(String key, String name, DictionaryType dictionaryType, Object defaultValue,
                                    ExpressionSupport expressionSupport, boolean required, DslElementSyntax paramDsl,
                                    Set<ModelProperty> modelProperties) {
    parseAttributeParameter(key, name, dictionaryType, defaultValue, expressionSupport, required, modelProperties);

    Class<? extends Map> mapType = getType(dictionaryType);
    if (ConcurrentMap.class.equals(mapType)) {
      mapType = ConcurrentHashMap.class;
    } else if (Map.class.equals(mapType)) {
      mapType = LinkedHashMap.class;
    }

    final MetadataType keyType = dictionaryType.getKeyType();
    final MetadataType valueType = dictionaryType.getValueType();
    final Class<?> keyClass = getType(keyType);
    final Class<?> valueClass = getType(valueType);
    final String parameterName = paramDsl.getAttributeName();
    final String mapElementName = paramDsl.getElementName();

    addParameter(getChildKey(key), fromChildMapConfiguration(keyClass, valueClass).withWrapperIdentifier(mapElementName)
        .withDefaultValue(defaultValue));

    addDefinition(baseDefinitionBuilder.copy().withIdentifier(mapElementName).withTypeDefinition(fromType(mapType)).build());


    Optional<DslElementSyntax> mapValueChildDsl = paramDsl.getGeneric(valueType);
    if (!mapValueChildDsl.isPresent()) {
      return;
    }

    DslElementSyntax valueDsl = mapValueChildDsl.get();
    valueType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        addDefinition(baseDefinitionBuilder.copy()
            .withIdentifier(valueDsl.getElementName())
            .withTypeDefinition(fromMapEntryType(keyClass, valueClass))
            .withKeyTypeConverter(value -> resolverOf(parameterName, keyType, value, null, expressionSupport, true,
                                                      modelProperties, false))
            .withTypeConverter(value -> resolverOf(parameterName, valueType, value, null, expressionSupport, true,
                                                   modelProperties, false))
            .build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        defaultVisit(objectType);
        if ((valueDsl.supportsTopLevelDeclaration() || (valueDsl.supportsChildDeclaration() && !valueDsl.isWrapped())) &&
            !parsingContext.isRegistered(valueDsl.getElementName(), valueDsl.getNamespace())) {
          try {
            parsingContext.registerObjectType(valueDsl.getElementName(), valueDsl.getNamespace(), objectType);
            new ObjectTypeParameterParser(baseDefinitionBuilder.copy(), objectType, getContextClassLoader(), dslResolver,
                                          parsingContext, muleContext).parse().forEach(definition -> addDefinition(definition));
          } catch (ConfigurationException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not create parser for map complex type"), e);
          }
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        defaultVisit(arrayType);

        Optional<DslElementSyntax> valueListGenericDsl = valueDsl.getGeneric(arrayType.getType());
        if (valueDsl.supportsChildDeclaration() && valueListGenericDsl.isPresent()) {
          arrayType.getType().accept(new BasicTypeMetadataVisitor() {

            @Override
            protected void visitBasicType(MetadataType metadataType) {
              addDefinition(baseDefinitionBuilder.copy().withIdentifier(valueListGenericDsl.get().getElementName())
                  .withTypeDefinition(fromType(getType(metadataType)))
                  .withTypeConverter(
                                     value -> resolverOf(parameterName, metadataType, value, getDefaultValue(metadataType),
                                                         getExpressionSupport(metadataType), false, modelProperties))
                  .build());
            }

            @Override
            protected void defaultVisit(MetadataType metadataType) {
              addDefinition(baseDefinitionBuilder.copy().withIdentifier(valueListGenericDsl.get().getElementName())
                  .withTypeDefinition(fromType(ValueResolver.class))
                  .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                  .withConstructorParameterDefinition(fromFixedValue(arrayType.getType()).build())
                  .withConstructorParameterDefinition(fromFixedValue(getContextClassLoader()).build()).build());
            }
          });
        }
      }
    });
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an {@link ArrayType}
   *
   * @param parameter a {@link ParameterModel}
   * @param arrayType an {@link ArrayType}
   */
  protected void parseCollectionParameter(ParameterModel parameter, ArrayType arrayType, DslElementSyntax parameterDsl) {
    parseCollectionParameter(getKey(parameter), parameter.getName(), arrayType, parameter.getDefaultValue(),
                             parameter.getExpressionSupport(), parameter.isRequired(), parameterDsl,
                             parameter.getModelProperties());
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
  protected void parseCollectionParameter(String key, String name, ArrayType arrayType, Object defaultValue,
                                          ExpressionSupport expressionSupport, boolean required, DslElementSyntax parameterDsl,
                                          Set<ModelProperty> modelProperties) {

    parseAttributeParameter(key, name, arrayType, defaultValue, expressionSupport, required, modelProperties);

    Class<? extends Iterable> collectionType = getType(arrayType);

    if (Set.class.equals(collectionType)) {
      collectionType = HashSet.class;
    } else if (Collection.class.equals(collectionType) || Iterable.class.equals(collectionType) || collectionType == null) {
      collectionType = List.class;
    }

    final String collectionElementName = parameterDsl.getElementName();
    addParameter(getChildKey(key), fromChildConfiguration(collectionType).withWrapperIdentifier(collectionElementName));
    addDefinition(baseDefinitionBuilder.copy().withIdentifier(collectionElementName).withTypeDefinition(fromType(collectionType))
        .build());


    Optional<DslElementSyntax> collectionItemDsl = parameterDsl.getGeneric(arrayType.getType());
    if (parameterDsl.supportsChildDeclaration() && collectionItemDsl.isPresent()) {
      String itemIdentifier = collectionItemDsl.get().getElementName();
      String itemNamespace = collectionItemDsl.get().getNamespace();

      arrayType.getType().accept(new BasicTypeMetadataVisitor() {

        private void addBasicTypeDefinition(MetadataType metadataType) {
          Builder itemDefinitionBuilder = baseDefinitionBuilder.copy().withIdentifier(itemIdentifier).withNamespace(itemNamespace)
              .withTypeDefinition(fromType(getType(metadataType)))
              .withTypeConverter(value -> resolverOf(name, metadataType, value, getDefaultValue(metadataType).orElse(null),
                                                     getExpressionSupport(metadataType), false, modelProperties));
          addDefinition(itemDefinitionBuilder.build());
        }

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          addBasicTypeDefinition(metadataType);
        }

        @Override
        public void visitDate(DateType dateType) {
          addBasicTypeDefinition(dateType);
        }

        @Override
        public void visitDateTime(DateTimeType dateTimeType) {
          addBasicTypeDefinition(dateTimeType);
        }

        @Override
        public void visitObject(ObjectType objectType) {
          DslElementSyntax itemDsl = collectionItemDsl.get();
          if ((itemDsl.supportsTopLevelDeclaration() || itemDsl.supportsChildDeclaration()) &&
              !parsingContext.isRegistered(itemDsl.getElementName(), itemDsl.getNamespace())) {
            try {
              parsingContext.registerObjectType(itemDsl.getElementName(), itemDsl.getNamespace(), objectType);
              new ObjectTypeParameterParser(baseDefinitionBuilder.copy(), objectType, getContextClassLoader(), dslResolver,
                                            parsingContext, muleContext).parse().forEach(definition -> addDefinition(definition));
            } catch (ConfigurationException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not create parser for collection complex type"), e);
            }
          }

        }
      });
    }

  }

  private ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  private ValueResolver<?> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue,
                                      ExpressionSupport expressionSupport, boolean required, Set<ModelProperty> modelProperties) {
    return resolverOf(parameterName, expectedType, value, defaultValue, expressionSupport, required, modelProperties, true);
  }

  protected ValueResolver<?> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue,
                                        ExpressionSupport expressionSupport, boolean required, Set<ModelProperty> modelProperties,
                                        boolean acceptsReferences) {
    if (value instanceof ValueResolver) {
      return (ValueResolver<?>) value;
    }

    ValueResolver resolver = null;

    if (isExpressionFunction(modelProperties) && value != null) {
      resolver =
          new ExpressionFunctionValueResolver<>((String) value, expectedType, muleContext);
    }
    if (isExpressionResolver(modelProperties) && value != null) {
      resolver =
          new ExpressionBasedParameterResolverValueResolver<>((String) value, expectedType, muleContext);
    }

    final Class<Object> expectedClass = getType(expectedType);
    if (resolver == null) {
      if (isExpression(value, parser)) {
        resolver = new TypeSafeExpressionValueResolver((String) value, expectedClass, muleContext);
      }
    }

    if (resolver == null && value != null) {
      final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
      expectedType.accept(new BasicTypeMetadataVisitor() {

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          if (conversionService.canConvert(value.getClass(), expectedClass)) {
            resolverValueHolder.set(new StaticValueResolver(convertSimpleValue(value, expectedClass, parameterName)));
          } else {
            defaultVisit(metadataType);
          }
        }

        @Override
        public void visitDateTime(DateTimeType dateTimeType) {
          resolverValueHolder.set(parseDate(value, dateTimeType, defaultValue));
        }

        @Override
        public void visitDate(DateType dateType) {
          resolverValueHolder.set(parseDate(value, dateType, defaultValue));
        }

        @Override
        public void visitObject(ObjectType objectType) {

          ValueResolver valueResolver;
          Optional<? extends ParsingDelegate> delegate = locateParsingDelegate(valueResolverParsingDelegates, objectType);
          Optional<DslElementSyntax> typeDsl = dslResolver.resolve(objectType);

          if (delegate.isPresent() && typeDsl.isPresent()) {
            valueResolver = (ValueResolver) delegate.get().parse(value.toString(), objectType, typeDsl.get(), muleContext);
          } else {
            valueResolver = acceptsReferences
                ? defaultValueResolverParsingDelegate.parse(value.toString(), objectType, null, muleContext)
                : new StaticValueResolver<>(value);
          }

          resolverValueHolder.set(valueResolver);
        }

        @Override
        protected void defaultVisit(MetadataType metadataType) {
          ValueResolver delegateResolver = locateParsingDelegate(valueResolverParsingDelegates, metadataType)
              .map(delegate -> delegate.parse(value.toString(), metadataType, null, muleContext))
              .orElseGet(() -> acceptsReferences
                  ? defaultValueResolverParsingDelegate.parse(value.toString(), metadataType, null, muleContext)
                  : new StaticValueResolver<>(value));

          resolverValueHolder.set(delegateResolver);
        }
      });

      resolver = resolverValueHolder.get();
    }

    if (resolver == null) {
      resolver = new StaticValueResolver<>(defaultValue);
    }

    if (resolver.isDynamic() && expressionSupport == NOT_SUPPORTED) {
      throw new IllegalArgumentException(
                                         format("An expression value was given for parameter '%s' but it doesn't support expressions",
                                                parameterName));
    }

    if (!resolver.isDynamic() && expressionSupport == REQUIRED && required) {
      throw new IllegalArgumentException(format("A fixed value was given for parameter '%s' but it only supports expressions",
                                                parameterName));
    }

    return resolver;
  }

  protected void parseFromTextExpression(ParameterModel parameter, DslElementSyntax paramDsl,
                                         Supplier<TypeConverter> typeConverter) {
    parseFromTextExpression(getKey(parameter), paramDsl, typeConverter);
  }

  protected void parseFromTextExpression(String key, DslElementSyntax paramDsl, Supplier<TypeConverter> typeConverter) {
    addParameter(getChildKey(key),
                 fromChildConfiguration(String.class).withWrapperIdentifier(paramDsl.getElementName()));

    addDefinition(baseDefinitionBuilder.copy()
        .withIdentifier(paramDsl.getElementName())
        .withTypeDefinition(fromType(String.class))
        .withTypeConverter(typeConverter.get())
        .build());
  }

  private boolean isExpression(Object value, TemplateParser parser) {
    return value instanceof String && parser.isContainsTemplate((String) value);
  }

  /**
   * Registers a definition for parsing the given {@code parameterModel} as an element attribute
   *
   * @param parameterModel a {@link ParameterModel}
   * @return an {@link AttributeDefinition.Builder}
   */
  protected AttributeDefinition.Builder parseAttributeParameter(ParameterModel parameterModel) {
    return parseAttributeParameter(getKey(parameterModel), parameterModel.getName(), parameterModel.getType(),
                                   parameterModel.getDefaultValue(), parameterModel.getExpressionSupport(),
                                   parameterModel.isRequired(), parameterModel.getModelProperties());
  }

  /**
   * Registers a definition for parsing the given {@code parameterModel} as an element attribute
   *
   * @param key               the key that the parsed value should have on the parsed parameter's map
   * @param name              the parameter's name
   * @param type              the parameter's type
   * @param defaultValue      the parameter's default value
   * @param expressionSupport the parameter's {@link ExpressionSupport}
   * @param required          whether the parameter is required or not
   * @return an {@link AttributeDefinition.Builder}
   */
  protected AttributeDefinition.Builder parseAttributeParameter(String key, String name, MetadataType type, Object defaultValue,
                                                                ExpressionSupport expressionSupport, boolean required,
                                                                Set<ModelProperty> modelProperties) {
    return parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required, true, modelProperties);
  }

  private AttributeDefinition.Builder parseAttributeParameter(String key, String name, MetadataType type, Object defaultValue,
                                                              ExpressionSupport expressionSupport, boolean required,
                                                              boolean acceptsReferences, Set<ModelProperty> modelProperties) {
    AttributeDefinition.Builder definitionBuilder =
        fromSimpleParameter(name, value -> resolverOf(name, type, value, defaultValue, expressionSupport, required,
                                                      modelProperties, acceptsReferences))
                                                          .withDefaultValue(defaultValue);

    addParameter(key, definitionBuilder);

    return definitionBuilder;
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an {@link ObjectType}
   *
   * @param parameterModel a {@link ParameterModel}
   */
  protected void parseObjectParameter(ParameterModel parameterModel, DslElementSyntax paramDsl) {
    if (isContent(parameterModel)) {
      parseFromTextExpression(parameterModel, paramDsl, () -> value -> resolverOf(
                                                                                  parameterModel.getName(),
                                                                                  parameterModel.getType(),
                                                                                  value,
                                                                                  parameterModel.getDefaultValue(),
                                                                                  parameterModel.getExpressionSupport(),
                                                                                  parameterModel.isRequired(),
                                                                                  parameterModel.getModelProperties(),
                                                                                  acceptsReferences(parameterModel)));
    } else {
      parseObjectParameter(getKey(parameterModel), parameterModel.getName(), (ObjectType) parameterModel.getType(),
                           parameterModel.getDefaultValue(), parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                           acceptsReferences(parameterModel), paramDsl, parameterModel.getModelProperties());
    }
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
   * @param modelProperties   parameter's {@link ModelProperty}s
   */
  protected void parseObjectParameter(String key, String name, ObjectType type, Object defaultValue,
                                      ExpressionSupport expressionSupport, boolean required,
                                      boolean acceptsReferences, DslElementSyntax elementDsl,
                                      Set<ModelProperty> modelProperties) {

    parseObject(key, name, type, defaultValue, expressionSupport, required, acceptsReferences, elementDsl, modelProperties);

    final String elementNamespace = elementDsl.getNamespace();
    final String elementName = elementDsl.getElementName();

    if (elementDsl.supportsChildDeclaration() && !elementDsl.isWrapped()
        && !parsingContext.isRegistered(elementName, elementNamespace)) {
      try {
        new ObjectTypeParameterParser(baseDefinitionBuilder.copy(), elementName, elementNamespace, type, getContextClassLoader(),
                                      dslResolver, parsingContext, muleContext).parse()
                                          .forEach(this::addDefinition);
      } catch (Exception e) {
        throw new MuleRuntimeException(new ConfigurationException(e));
      }
    }
  }

  protected void parseObject(String key, String name, ObjectType type, Object defaultValue, ExpressionSupport expressionSupport,
                             boolean required, boolean acceptsReferences, DslElementSyntax elementDsl,
                             Set<ModelProperty> modelProperties) {
    parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required, acceptsReferences, modelProperties);

    ObjectParsingDelegate delegate = (ObjectParsingDelegate) locateParsingDelegate(objectParsingDelegates, type)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find a parsing delegate for type "
            + getType(type).getName())));

    addParameter(getChildKey(key), delegate.parse(name, type, elementDsl, muleContext));
  }

  private <M extends MetadataType, T> Optional<ParsingDelegate<M, T>> locateParsingDelegate(
                                                                                            List<? extends ParsingDelegate<M, T>> delegatesList,
                                                                                            M metadataType) {
    return (Optional<ParsingDelegate<M, T>>) delegatesList.stream().filter(candidate -> candidate.accepts(metadataType))
        .findFirst();
  }

  /**
   * Adds the given {@code definition} to the list of definitions that the {@link #parse()} method generates by default
   *
   * @param definition a {@link ComponentBuildingDefinition}
   */
  protected void addDefinition(ComponentBuildingDefinition definition) {
    parsedDefinitions.add(definition);
  }

  protected void addParameter(String key, AttributeDefinition.Builder definitionBuilder) {
    if (parameters.put(key, definitionBuilder) != null) {
      throw new IllegalArgumentException("An AttributeDefinition builder was already defined for parameter " + key);
    }
  }

  private void parseNestedProcessor(ParameterModel parameterModel) {
    final String processorElementName = hyphenize(parameterModel.getName());
    addParameter(getChildKey(parameterModel.getName()),
                 fromChildConfiguration(NestedProcessorValueResolver.class).withWrapperIdentifier(processorElementName));

    addDefinition(baseDefinitionBuilder.copy().withIdentifier(processorElementName)
        .withTypeDefinition(fromType(NestedProcessorValueResolver.class))
        .withConstructorParameterDefinition(fromChildConfiguration(Processor.class).build()).build());
  }

  private void parseNestedProcessorList(ParameterModel parameterModel) {
    final String processorElementName = hyphenize(parameterModel.getName());
    addParameter(getChildKey(parameterModel.getName()), fromChildCollectionConfiguration(NestedProcessorListValueResolver.class)
        .withWrapperIdentifier(processorElementName));

    addDefinition(baseDefinitionBuilder.copy().withIdentifier(processorElementName)
        .withTypeDefinition(fromType(NestedProcessorListValueResolver.class))
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Processor.class).build())
        .build());
  }

  private boolean isExpressionFunction(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(property -> property instanceof FunctionParameterTypeModelProperty);
  }

  private boolean isExpressionResolver(Set<ModelProperty> modelProperties) {
    return modelProperties
        .stream()
        .anyMatch(property -> property instanceof ParameterResolverTypeModelProperty);
  }

  private ValueResolver doParseDate(Object value, Class<?> type) {

    if (value instanceof String) {
      Object constructedValue = null;
      DateTime dateTime = getParsedDateTime((String) value);

      if (type.equals(LocalDate.class)) {
        constructedValue = LocalDate.of(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
      } else if (type.equals(Date.class)) {
        constructedValue = dateTime.toDate();
      } else if (type.equals(LocalDateTime.class)) {
        Instant instant = ofEpochMilli(dateTime.getMillis());
        constructedValue = LocalDateTime.ofInstant(instant, ZoneId.of(dateTime.getZone().getID()));
      } else if (type.equals(Calendar.class)) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime.toDate());
        constructedValue = calendar;
      }

      if (constructedValue == null) {
        throw new IllegalArgumentException(format("Could not construct value of type '%s' from String '%s'", type.getName(),
                                                  value));
      } else {
        value = constructedValue;
      }
    }

    if (value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Calendar) {
      return new StaticValueResolver<>(value);
    }

    throw new IllegalArgumentException(format("Could not transform value of type '%s' to a valid date type",
                                              value != null ? value.getClass().getName() : "null"));
  }

  private DateTime getParsedDateTime(String value) {
    try {
      return ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(value);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(format("Could not parse value '%s' according to ISO 8601", value));
    }
  }

  private ValueResolver parseDate(Object value, MetadataType dateType, Object defaultValue) {

    Class<?> type = getType(dateType);
    if (isExpression(value, parser)) {
      return new TypeSafeExpressionValueResolver<>((String) value, type, muleContext);
    }

    if (value == null) {
      if (defaultValue == null) {
        return new StaticValueResolver<>(null);
      }

      value = defaultValue;
    }

    return doParseDate(value, type);
  }


  private String getKey(ParameterModel parameterModel) {
    return getMemberName(parameterModel, parameterModel.getName());
  }

  private String getChildKey(String key) {
    return format("%s%s%s", CHILD_ELEMENT_KEY_PREFIX, key, CHILD_ELEMENT_KEY_SUFFIX);
  }

  private Object convertSimpleValue(Object value, Class<Object> expectedClass, String parameterName) {
    try {
      return conversionService.convert(value, expectedClass);
    } catch (Exception e) {
      throw new IllegalArgumentException(format("Could not transform simple value '%s' to type '%s' in parameter '%s'", value,
                                                expectedClass.getSimpleName(), parameterName));
    }
  }

  private boolean acceptsReferences(ParameterModel parameterModel) {
    return parameterModel.getDslModel().allowsReferences();
  }
}
