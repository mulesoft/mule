/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping.getNameMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isLiteral;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTargetParameter;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.internal.config.dsl.construct.RouteComponentParser;
import org.mule.runtime.module.extension.internal.config.dsl.object.CharsetValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.FixedTypeParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.MediaTypeValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ValueResolverParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.AnonymousInlineParameterGroupParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.ObjectTypeParameterParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TopLevelParameterObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TypedInlineParameterGroupParser;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedParameterResolverValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionTypedValueValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NativeQueryParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ProcessorChainValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RequiredParameterValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticLiteralValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

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

import com.google.common.collect.ImmutableList;
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
  protected static final String CURSOR_PROVIDER_FACTORY_FIELD_NAME = "cursorProviderFactory";

  protected final ExtensionParsingContext parsingContext;
  protected final List<ObjectParsingDelegate> objectParsingDelegates = ImmutableList
      .of(new FixedTypeParsingDelegate(PoolingProfile.class), new FixedTypeParsingDelegate(RetryPolicyTemplate.class),
          new FixedTypeParsingDelegate(TlsContextFactory.class), new DefaultObjectParsingDelegate());
  protected final DslSyntaxResolver dslResolver;
  protected final Builder baseDefinitionBuilder;
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();
  private final ConversionService conversionService = new DefaultConversionService();
  private final Map<String, AttributeDefinition.Builder> parameters = new HashMap<>();
  private final List<ComponentBuildingDefinition> parsedDefinitions = new ArrayList<>();
  private final List<ValueResolverParsingDelegate> valueResolverParsingDelegates =
      ImmutableList.of(new CharsetValueResolverParsingDelegate(), new MediaTypeValueResolverParsingDelegate());
  private final ValueResolverParsingDelegate defaultValueResolverParsingDelegate = new DefaultValueResolverParsingDelegate();
  protected final Map<String, String> infrastructureParameterMap = getNameMap();
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  /**
   * Creates a new instance
   *
   * @param baseDefinitionBuilder a {@link Builder} used as a prototype to generate new definitions
   * @param dslSyntaxResolver     a {@link DslSyntaxResolver} instance associated with the {@link ExtensionModel} being parsed
   * @param parsingContext        the {@link ExtensionParsingContext} in which {@code this} parser operates
   */
  protected ExtensionDefinitionParser(Builder baseDefinitionBuilder, DslSyntaxResolver dslSyntaxResolver,
                                      ExtensionParsingContext parsingContext) {
    this.baseDefinitionBuilder = baseDefinitionBuilder;
    this.dslResolver = dslSyntaxResolver;
    this.parsingContext = parsingContext;
  }

  /**
   * Creates a list of {@link ComponentBuildingDefinition} built on copies of {@link #baseDefinitionBuilder}. It also sets the
   * parsed parsed parameters on the backing {@link AbstractExtensionObjectFactory}
   *
   * @return a list with the generated {@link ComponentBuildingDefinition}
   * @throws ConfigurationException if a parsing error occurs
   */
  public final List<ComponentBuildingDefinition> parse() throws ConfigurationException {
    Builder builder = baseDefinitionBuilder;
    builder = doParse(builder);

    AttributeDefinition parametersDefinition = fromFixedValue(new HashMap<>()).build();
    if (!parameters.isEmpty()) {
      KeyAttributeDefinitionPair[] attributeDefinitions = parameters.entrySet().stream()
          .map(entry -> newBuilder().withAttributeDefinition(entry.getValue().build()).withKey(entry.getKey()).build())
          .toArray(KeyAttributeDefinitionPair[]::new);
      parametersDefinition = fromMultipleDefinitions(attributeDefinitions).build();
    }

    builder = builder.withSetterParameterDefinition("parameters", parametersDefinition);

    addDefinition(builder.build());
    return parsedDefinitions;
  }

  /**
   * Implementations place their custom parsing logic here.
   *
   * @param definitionBuilder the {@link Builder} on which implementation are to define their stuff
   * @throws ConfigurationException if a parsing error occurs
   */
  protected abstract Builder doParse(Builder definitionBuilder) throws ConfigurationException;

  /**
   * Parses the given {@code nestedComponents} and generates matching definitions  
   *
   * @param nestedComponents a list of {@link NestableElementModel}
   */
  protected void parseNestedComponents(List<? extends NestableElementModel> nestedComponents) {
    nestedComponents.forEach(component -> component.accept(new NestableElementModelVisitor() {

      @Override
      public void visit(NestedChainModel component) {
        parseProcessorChain(component);
      }

      @Override
      public void visit(NestedComponentModel component) {
        // not yet implemented, support this when SDK the user to receive a single component
      }

      @Override
      public void visit(NestedRouteModel component) {
        parseRoute(component);
      }
    }));
  }

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
          if (!parseAsContent(metadataType)) {
            parseAttributeParameter(parameter);
          }
        }

        @Override
        public void visitString(StringType stringType) {
          if (paramDsl.supportsChildDeclaration()) {
            parseFromTextExpression(parameter, paramDsl, () -> {
              Optional<QueryParameterModelProperty> query = parameter.getModelProperty(QueryParameterModelProperty.class);
              return value -> {
                ValueResolver<String> resolver = resolverOf(parameter.getName(), stringType, value, parameter.getDefaultValue(),
                                                            parameter.getExpressionSupport(), parameter.isRequired(),
                                                            parameter.getModelProperties(), acceptsReferences(parameter));

                return query
                    .map(p -> (ValueResolver<String>) new NativeQueryParameterValueResolver(resolver, p.getQueryTranslator()))
                    .orElse(resolver);
              };
            });
          } else {
            defaultVisit(stringType);
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          if (parseAsContent(objectType)) {
            return;
          }

          if (isMap(objectType)) {
            parseMapParameters(parameter, objectType, paramDsl);
            return;
          }

          if (!parsingContext.isRegistered(paramDsl.getElementName(), paramDsl.getPrefix())) {
            parsingContext.registerObjectType(paramDsl.getElementName(), paramDsl.getPrefix(), objectType);
            parseObjectParameter(parameter, paramDsl);
          } else {
            parseObject(getKey(parameter), parameter.getName(), objectType, parameter.getDefaultValue(),
                        parameter.getExpressionSupport(), parameter.isRequired(), acceptsReferences(parameter),
                        paramDsl, parameter.getModelProperties());
          }

        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (!parseAsContent(arrayType)) {
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

      });
    });
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an open {@link ObjectType}
   *
   * @param parameter  a {@link ParameterModel}
   * @param objectType a {@link ObjectType}
   */
  protected void parseMapParameters(ParameterModel parameter, ObjectType objectType, DslElementSyntax paramDsl) {
    parseMapParameters(getKey(parameter), parameter.getName(), objectType, parameter.getDefaultValue(),
                       parameter.getExpressionSupport(), parameter.isRequired(), paramDsl, parameter.getModelProperties());
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an open {@link ObjectType}
   *
   * @param key               the key that the parsed value should have on the parsed parameter's map
   * @param name              the parameter's name
   * @param dictionaryType    the parameter's open {@link ObjectType}
   * @param defaultValue      the parameter's default value
   * @param expressionSupport the parameter's {@link ExpressionSupport}
   * @param required          whether the parameter is required
   */
  protected void parseMapParameters(String key, String name, ObjectType dictionaryType, Object defaultValue,
                                    ExpressionSupport expressionSupport, boolean required, DslElementSyntax paramDsl,
                                    Set<ModelProperty> modelProperties) {
    parseAttributeParameter(key, name, dictionaryType, defaultValue, expressionSupport, required, modelProperties);

    Class<? extends Map> mapType = getType(dictionaryType);
    if (ConcurrentMap.class.equals(mapType)) {
      mapType = ConcurrentHashMap.class;
    } else if (Map.class.equals(mapType)) {
      mapType = LinkedHashMap.class;
    }

    final MetadataType valueType = dictionaryType.getOpenRestriction().get();
    final Class<?> valueClass = getType(valueType);
    final MetadataType keyType = typeLoader.load(String.class);
    final Class<?> keyClass = String.class;

    final String mapElementName = paramDsl.getElementName();

    addParameter(getChildKey(key), fromChildMapConfiguration(String.class, valueClass).withWrapperIdentifier(mapElementName)
        .withDefaultValue(defaultValue));

    addDefinition(baseDefinitionBuilder.withIdentifier(mapElementName).withTypeDefinition(fromType(mapType)).build());


    Optional<DslElementSyntax> mapValueChildDsl = paramDsl.getGeneric(valueType);
    if (!mapValueChildDsl.isPresent()) {
      return;
    }

    DslElementSyntax valueDsl = mapValueChildDsl.get();
    valueType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        String parameterName = paramDsl.getAttributeName();

        addDefinition(baseDefinitionBuilder
            .withIdentifier(valueDsl.getElementName())
            .withTypeDefinition(fromMapEntryType(keyClass, valueClass))
            .withKeyTypeConverter(value -> resolverOf(parameterName, keyType, value, null, expressionSupport, true,
                                                      emptySet(), false))
            .withTypeConverter(value -> resolverOf(parameterName, valueType, value, null, expressionSupport, true,
                                                   emptySet(), false))
            .build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        defaultVisit(objectType);
        Optional<DslElementSyntax> containedElement = valueDsl.getContainedElement(VALUE_ATTRIBUTE_NAME);
        if (isMap(objectType) || !containedElement.isPresent()) {
          return;
        }

        DslElementSyntax valueChild = containedElement.get();
        if ((valueChild.supportsTopLevelDeclaration() || (valueChild.supportsChildDeclaration() && !valueChild.isWrapped())) &&
            !parsingContext.isRegistered(valueChild.getElementName(), valueChild.getPrefix())) {
          try {
            parsingContext.registerObjectType(valueChild.getElementName(), valueChild.getPrefix(), objectType);
            new ObjectTypeParameterParser(baseDefinitionBuilder, objectType, getContextClassLoader(), dslResolver,
                                          parsingContext).parse().forEach(definition -> addDefinition(definition));
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
              String parameterName = paramDsl.getAttributeName();
              addDefinition(baseDefinitionBuilder.withIdentifier(valueListGenericDsl.get().getElementName())
                  .withTypeDefinition(fromType(getType(metadataType)))
                  .withTypeConverter(
                                     value -> resolverOf(parameterName, metadataType, value, getDefaultValue(metadataType),
                                                         getExpressionSupport(metadataType), false, emptySet()))
                  .build());
            }

            @Override
            protected void defaultVisit(MetadataType metadataType) {
              addDefinition(baseDefinitionBuilder.withIdentifier(valueListGenericDsl.get().getElementName())
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

  protected void parseFields(ObjectType type, DslElementSyntax typeDsl, Map<String, ParameterRole> parametersRole) {
    type.getFields().forEach(f -> parseField(type, typeDsl, f, parametersRole));
  }

  protected void parseFields(ObjectType type, DslElementSyntax typeDsl) {
    type.getFields().forEach(f -> parseField(type, typeDsl, f, emptyMap()));
  }

  private void parseField(ObjectType type, DslElementSyntax typeDsl, ObjectFieldType objectField,
                          Map<String, ParameterRole> parametersRole) {
    final MetadataType fieldType = objectField.getValue();
    final String fieldName = objectField.getKey().getName().getLocalPart();
    final boolean acceptsReferences = ExtensionMetadataTypeUtils.acceptsReferences(objectField);
    final Object defaultValue = getDefaultValue(fieldType).orElse(null);
    final ExpressionSupport expressionSupport = getExpressionSupport(objectField);
    Optional<DslElementSyntax> fieldDsl = typeDsl.getContainedElement(fieldName);
    if (!fieldDsl.isPresent() && !isFlattenedParameterGroup(objectField)) {
      return;
    }

    Optional<String> keyName = getInfrastructureParameterName(fieldType);
    if (keyName.isPresent()) {
      parseObject(fieldName, keyName.get(), (ObjectType) fieldType, defaultValue, expressionSupport, false, acceptsReferences,
                  fieldDsl.get(), emptySet());
      return;
    }

    final boolean isContent = isContent(parametersRole.getOrDefault(fieldName, BEHAVIOUR));
    fieldType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (!parseAsContent(isContent, metadataType)) {
          parseAttributeParameter(fieldName, fieldName, metadataType, defaultValue, expressionSupport, false, emptySet());
        }
      }

      @Override
      public void visitString(StringType stringType) {
        if (fieldDsl.get().supportsChildDeclaration()) {
          String elementName = fieldDsl.get().getElementName();
          addParameter(fieldName, fromChildConfiguration(String.class).withWrapperIdentifier(elementName));
          addDefinition(baseDefinitionBuilder
              .withIdentifier(elementName)
              .withTypeDefinition(fromType(String.class))
              .withTypeConverter(value -> resolverOf(elementName, stringType, value, defaultValue,
                                                     expressionSupport, false,
                                                     emptySet(), acceptsReferences))
              .build());
        } else {
          defaultVisit(stringType);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (objectType.isOpen()) {
          if (!parseAsContent(isContent, objectType)) {
            parseMapParameters(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, fieldDsl.get(),
                               emptySet());
          }
          return;
        }

        if (isFlattenedParameterGroup(objectField)) {
          dslResolver.resolve(objectType)
              .ifPresent(objectDsl -> objectType.getFields()
                  .forEach(field -> parseField(objectType, objectDsl, field, parametersRole)));
          return;
        }

        if (parseAsContent(isContent, objectType)) {
          return;
        }

        DslElementSyntax dsl = fieldDsl.get();
        if (!parsingContext.isRegistered(dsl.getElementName(), dsl.getPrefix())) {
          parsingContext.registerObjectType(dsl.getElementName(), dsl.getPrefix(), type);
          parseObjectParameter(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, acceptsReferences,
                               dsl, emptySet());
        } else {
          parseObject(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, acceptsReferences,
                      dsl, emptySet());
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (!parseAsContent(isContent, arrayType)) {
          parseCollectionParameter(fieldName, fieldName, arrayType, defaultValue, expressionSupport, false, fieldDsl.get(),
                                   emptySet());
        }
      }

      private boolean parseAsContent(boolean isContent, MetadataType type) {
        if (isContent) {
          parseFromTextExpression(fieldName, fieldDsl.get(), () -> value -> resolverOf(fieldName, type, value, defaultValue,
                                                                                       expressionSupport, false, emptySet(),
                                                                                       false));

          return true;
        }

        return false;
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

    Class<?> collectionType = ExtensionMetadataTypeUtils.getType(arrayType).orElse(null);

    if (Set.class.equals(collectionType)) {
      collectionType = HashSet.class;
    } else if (Collection.class.equals(collectionType) || Iterable.class.equals(collectionType) || collectionType == null) {
      collectionType = List.class;
    }

    final String collectionElementName = parameterDsl.getElementName();
    addParameter(getChildKey(key), fromChildConfiguration(collectionType).withWrapperIdentifier(collectionElementName));
    addDefinition(baseDefinitionBuilder.withIdentifier(collectionElementName).withTypeDefinition(fromType(collectionType))
        .build());


    Optional<DslElementSyntax> collectionItemDsl = parameterDsl.getGeneric(arrayType.getType());
    if (parameterDsl.supportsChildDeclaration() && collectionItemDsl.isPresent()) {
      String itemIdentifier = collectionItemDsl.get().getElementName();
      String itemNamespace = collectionItemDsl.get().getPrefix();

      arrayType.getType().accept(new BasicTypeMetadataVisitor() {

        private void addBasicTypeDefinition(MetadataType metadataType) {
          Builder itemDefinitionBuilder = baseDefinitionBuilder.withIdentifier(itemIdentifier).withNamespace(itemNamespace)
              .withTypeDefinition(fromType(ExtensionMetadataTypeUtils.getType(metadataType).orElse(Object.class)))
              .withTypeConverter(value -> resolverOf(name, metadataType, value, getDefaultValue(metadataType).orElse(null),
                                                     getExpressionSupport(metadataType), false, emptySet()));
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
          if (isMap(objectType)) {
            return;
          }

          DslElementSyntax itemDsl = collectionItemDsl.get();
          if ((itemDsl.supportsTopLevelDeclaration() || itemDsl.supportsChildDeclaration()) &&
              !parsingContext.isRegistered(itemDsl.getElementName(), itemDsl.getPrefix())) {
            try {
              parsingContext.registerObjectType(itemDsl.getElementName(), itemDsl.getPrefix(), objectType);
              new ObjectTypeParameterParser(baseDefinitionBuilder, objectType, getContextClassLoader(), dslResolver,
                                            parsingContext).parse().forEach(definition -> addDefinition(definition));
            } catch (ConfigurationException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not create parser for collection complex type"), e);
            }
          }

        }
      });
    }

  }

  protected ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  private <T> ValueResolver<T> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue,
                                          ExpressionSupport expressionSupport, boolean required,
                                          Set<ModelProperty> modelProperties) {
    return resolverOf(parameterName, expectedType, value, defaultValue, expressionSupport, required, modelProperties, true);
  }

  protected <T> ValueResolver<T> resolverOf(String parameterName, MetadataType expectedType, Object value, Object defaultValue,
                                            ExpressionSupport expressionSupport, boolean required,
                                            Set<ModelProperty> modelProperties,
                                            boolean acceptsReferences) {
    if (value instanceof ValueResolver) {
      return (ValueResolver<T>) value;
    }

    ValueResolver<T> resolver;

    final Class<Object> expectedClass = ExtensionMetadataTypeUtils.getType(expectedType).orElse(Object.class);

    if (isExpression(value, parser)) {
      final String expression = (String) value;
      resolver = getExpressionBasedValueResolver(expectedType, expression, modelProperties, expectedClass);
      if (required) {
        resolver = new RequiredParameterValueResolverWrapper(resolver, parameterName, expression);
      }
    } else {
      resolver = getStaticValueResolver(parameterName, expectedType, value, defaultValue, modelProperties, acceptsReferences,
                                        expectedClass);
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

  /**
   * Generates the {@link ValueResolver} for expression based values
   */
  private ValueResolver getExpressionBasedValueResolver(MetadataType expectedType, String value,
                                                        Set<ModelProperty> modelProperties, Class<Object> expectedClass) {
    ValueResolver resolver;
    Optional<StackedTypesModelProperty> stackedTypesModelProperty = getStackedTypesModelProperty(modelProperties);
    if (stackedTypesModelProperty.isPresent()) {
      resolver = stackedTypesModelProperty.get().getValueResolverFactory().getExpressionBasedValueResolver(value, expectedClass);
      //TODO MULE-13518: Add support for stacked value resolvers for @Parameter inside pojos // The following "IFs" should be removed once implemented
    } else if (isParameterResolver(expectedType)) {
      resolver = new ExpressionBasedParameterResolverValueResolver<>(value, expectedType);
    } else if (isTypedValue(expectedType)) {
      resolver = new ExpressionTypedValueValueResolver<>(value, expectedClass);
    } else if (isLiteral(expectedType) || isTargetParameter(modelProperties)) {
      resolver = new StaticLiteralValueResolver<>(value, expectedClass);
    } else {
      resolver = new TypeSafeExpressionValueResolver(value, expectedType);
    }
    return resolver;
  }

  /**
   * Generates the {@link ValueResolver} for non expression based values
   */
  private ValueResolver getStaticValueResolver(String parameterName, MetadataType expectedType, Object value, Object defaultValue,
                                               Set<ModelProperty> modelProperties, boolean acceptsReferences,
                                               Class<Object> expectedClass) {

    Optional<StackedTypesModelProperty> optionalStackedTypeModelProperty = getStackedTypesModelProperty(modelProperties);

    if (optionalStackedTypeModelProperty.isPresent()) {
      StackedTypesModelProperty property = optionalStackedTypeModelProperty.get();
      Optional<ValueResolver> optionalResolver = property.getValueResolverFactory().getStaticValueResolver(value, Literal.class);
      if (optionalResolver.isPresent()) {
        return optionalResolver.get();
      }
    }

    if (isLiteral(expectedType)) {
      return new StaticLiteralValueResolver<>(value != null ? value.toString() : null, expectedClass);
    }

    ValueResolver resolver;
    resolver = value != null
        ? getValueResolverFromMetadataType(parameterName, expectedType, value, defaultValue, acceptsReferences, expectedClass)
        : new StaticValueResolver<>(defaultValue);

    if (optionalStackedTypeModelProperty.isPresent()) {
      resolver = optionalStackedTypeModelProperty.get().getValueResolverFactory().getWrapperValueResolver(resolver);
    } else if (isParameterResolver(expectedType)) {
      resolver = new ParameterResolverValueResolverWrapper(resolver);
    } else if (isTypedValue(expectedType)) {
      resolver = new TypedValueValueResolverWrapper(resolver);
    }

    return resolver;
  }

  private ValueResolver getValueResolverFromMetadataType(final String parameterName, MetadataType expectedType,
                                                         final Object value,
                                                         final Object defaultValue, final boolean acceptsReferences,
                                                         final Class<Object> expectedClass) {
    final Reference<ValueResolver> resolverValueHolder = new Reference<>();
    expectedType.accept(new BasicTypeMetadataVisitor() {

      @Override
      protected void visitBasicType(MetadataType metadataType) {
        if (conversionService.canConvert(value.getClass(), expectedClass)) {
          resolverValueHolder.set(new StaticValueResolver<>(convertSimpleValue(value, expectedClass, parameterName)));
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
        if (isMap(objectType)) {
          defaultVisit(objectType);
          return;
        }

        ValueResolver valueResolver;
        Optional<? extends ParsingDelegate> delegate = locateParsingDelegate(valueResolverParsingDelegates, objectType);
        Optional<DslElementSyntax> typeDsl = dslResolver.resolve(objectType);

        if (delegate.isPresent() && typeDsl.isPresent()) {
          valueResolver = (ValueResolver) delegate.get().parse(value.toString(), objectType, typeDsl.get());
        } else {
          valueResolver = acceptsReferences
              ? defaultValueResolverParsingDelegate.parse(value.toString(), objectType, null)
              : new StaticValueResolver<>(value);
        }

        resolverValueHolder.set(valueResolver);
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        ValueResolver delegateResolver = locateParsingDelegate(valueResolverParsingDelegates, metadataType)
            .map(delegate -> delegate.parse(value.toString(), metadataType, null))
            .orElseGet(() -> acceptsReferences
                ? defaultValueResolverParsingDelegate.parse(value.toString(), metadataType, null)
                : new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(value), expectedClass));

        resolverValueHolder.set(delegateResolver);
      }
    });

    return resolverValueHolder.get();
  }

  protected void parseFromTextExpression(ParameterModel parameter, DslElementSyntax paramDsl,
                                         Supplier<TypeConverter> typeConverter) {
    parseFromTextExpression(getKey(parameter), paramDsl, typeConverter);
  }

  protected void parseFromTextExpression(String key, DslElementSyntax paramDsl, Supplier<TypeConverter> typeConverter) {
    addParameter(getChildKey(key),
                 fromChildConfiguration(String.class).withWrapperIdentifier(paramDsl.getElementName()));

    addDefinition(baseDefinitionBuilder
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
    AttributeDefinition.Builder definitionBuilder;

    if (acceptsReferences &&
        expressionSupport == NOT_SUPPORTED &&
        type instanceof ObjectType) {

      definitionBuilder = fromSimpleReferenceParameter(name);
    } else {
      definitionBuilder =
          fromSimpleParameter(name, value -> resolverOf(name, type, value, defaultValue, expressionSupport, required,
                                                        modelProperties, acceptsReferences));
    }

    definitionBuilder.withDefaultValue(defaultValue);
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
      parseFromTextExpression(parameterModel, paramDsl,
                              () -> value -> resolverOf(parameterModel.getName(),
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

    final String elementNamespace = elementDsl.getPrefix();
    final String elementName = elementDsl.getElementName();

    if (elementDsl.supportsChildDeclaration() && !elementDsl.isWrapped() &&
        modelProperties.stream().noneMatch(m -> m.getName().equals(InfrastructureParameterModelProperty.NAME))) {
      try {
        new ObjectTypeParameterParser(baseDefinitionBuilder, elementName, elementNamespace, type, getContextClassLoader(),
                                      dslResolver, parsingContext).parse()
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

    addParameter(getChildKey(key), delegate.parse(name, type, elementDsl));
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

  protected List<ParameterGroupModel> getInlineGroups(ParameterizedModel model) {
    return model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .collect(toList());
  }

  private void parseProcessorChain(NestedChainModel chainModel) {
    final String processorElementName = hyphenize(chainModel.getName());
    addParameter(getChildKey(chainModel.getName()),
                 fromChildConfiguration(ProcessorChainValueResolver.class).withWrapperIdentifier(processorElementName));

    addDefinition(baseDefinitionBuilder.withIdentifier(processorElementName)
        .withTypeDefinition(fromType(ProcessorChainValueResolver.class))
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Processor.class).build())
        .build());
  }

  private void parseRoute(NestedRouteModel routeModel) {
    DslElementSyntax routeDsl = dslResolver.resolve(routeModel);

    Class<?> type = routeModel.getModelProperty(ImplementingTypeModelProperty.class)
        .map(ImplementingTypeModelProperty::getType)
        .orElseThrow(() -> new IllegalStateException("Missing route information"));

    MetadataType metadataType = typeLoader.load(type);
    addParameter(getChildKey(routeModel.getName()),
                 new DefaultObjectParsingDelegate().parse(routeModel.getName(), (ObjectType) metadataType, routeDsl));

    try {
      new RouteComponentParser(baseDefinitionBuilder, routeModel, metadataType, getContextClassLoader(), routeDsl,
                               dslResolver, parsingContext).parse()
                                   .forEach(this::addDefinition);
    } catch (Exception e) {
      throw new MuleRuntimeException(new ConfigurationException(e));
    }
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
      return new TypeSafeExpressionValueResolver<>((String) value, dateType);
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

  protected String getChildKey(String key) {
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
    return parameterModel.getDslConfiguration().allowsReferences();
  }

  protected void parseParameters(ParameterizedModel parameterizedModel) throws ConfigurationException {
    List<ParameterGroupModel> inlineGroups = getInlineGroups(parameterizedModel);
    parseParameters(getFlatParameters(inlineGroups, parameterizedModel.getAllParameterModels()));

    for (ParameterGroupModel group : inlineGroups) {
      parseInlineParameterGroup(group);
    }
  }

  protected void parseInlineParameterGroup(ParameterGroupModel group)
      throws ConfigurationException {
    ParameterGroupDescriptor descriptor =
        group.getModelProperty(ParameterGroupModelProperty.class)
            .map(ParameterGroupModelProperty::getDescriptor)
            .orElse(null);

    DslElementSyntax dslElementSyntax = dslResolver.resolveInline(group);

    if (descriptor != null) {
      addParameter(getChildKey(getContainerName(descriptor.getContainer())),
                   new DefaultObjectParsingDelegate().parse("", null, dslElementSyntax));
      new TypedInlineParameterGroupParser(baseDefinitionBuilder, group, descriptor, getContextClassLoader(),
                                          dslElementSyntax,
                                          dslResolver, parsingContext).parse().forEach(this::addDefinition);
    } else {
      AttributeDefinition.Builder builder = fromChildConfiguration(Map.class);
      if (dslElementSyntax.isWrapped()) {
        builder.withWrapperIdentifier(dslElementSyntax.getElementName());
      } else {
        builder.withIdentifier(dslElementSyntax.getElementName());
      }
      addParameter(getChildKey(group.getName()), builder);
      new AnonymousInlineParameterGroupParser(baseDefinitionBuilder, group, getContextClassLoader(), dslElementSyntax,
                                              dslResolver, parsingContext).parse().forEach(this::addDefinition);
    }
  }

  protected List<ParameterModel> getFlatParameters(List<ParameterGroupModel> inlineGroups, List<ParameterModel> parameters) {
    List<ParameterModel> inlineParameters = inlineGroups.stream()
        .flatMap(g -> g.getParameterModels().stream()).collect(toList());


    return inlineParameters.isEmpty()
        ? parameters
        : parameters.stream()
            .filter(p -> !inlineParameters.contains(p))
            .collect(toList());
  }

  protected Optional<String> getInfrastructureParameterName(MetadataType fieldType) {
    return getId(fieldType).map(infrastructureParameterMap::get);
  }
}
