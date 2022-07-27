/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSoftReferenceSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isReferableType;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getNameMap;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingUtils.acceptsReferences;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingUtils.getChildKey;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingUtils.locateParsingDelegate;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.AnyType;
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
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.config.dsl.construct.RouteComponentParser;
import org.mule.runtime.module.extension.internal.config.dsl.object.DefaultObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.FixedTypeParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.ObjectParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.object.SchedulingStrategyParsingDelegate;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.AnonymousInlineParameterGroupParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.ObjectTypeParameterParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TopLevelParameterObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.TypedInlineParameterGroupParser;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.NativeQueryParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ProcessorChainValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;

import java.util.ArrayList;
import java.util.Collection;
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

  protected static final String CURSOR_PROVIDER_FACTORY_FIELD_NAME = "cursorProviderFactory";
  protected static final String PARAMETERS_FIELD_NAME = "parameters";

  protected final List<ObjectParsingDelegate> objectParsingDelegates = ImmutableList
      .of(new FixedTypeParsingDelegate(PoolingProfile.class),
          new FixedTypeParsingDelegate(RetryPolicyTemplate.class),
          new FixedTypeParsingDelegate(TlsContextFactory.class),
          new SchedulingStrategyParsingDelegate(),
          new DefaultObjectParsingDelegate());
  protected final DslSyntaxResolver dslResolver;
  private final Map<String, AttributeDefinition.Builder> parameters = new HashMap<>();
  private final List<ComponentBuildingDefinition> parsedDefinitions = new ArrayList<>();
  protected final Map<String, String> infrastructureParameterMap = getNameMap();
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  protected final Builder definitionBuilder;
  protected final ExtensionParsingContext parsingContext;
  protected final ValueResolverFactory valueResolverFactory;

  /**
   * Creates a new instance
   *
   * @param definitionBuilder a {@link Builder} used as a prototype to generate new definitions
   * @param dslResolver       a {@link DslSyntaxResolver} instance associated with the {@link ExtensionModel} being parsed
   * @param ctx               the {@link ExtensionParsingContext} in which {@code this} parser operates
   */
  protected ExtensionDefinitionParser(Builder definitionBuilder, DslSyntaxResolver dslResolver, ExtensionParsingContext ctx) {
    this.definitionBuilder = definitionBuilder;
    this.dslResolver = dslResolver;
    this.parsingContext = ctx;
    this.valueResolverFactory = new ValueResolverFactory();
  }

  /**
   * Creates a list of {@link ComponentBuildingDefinition} built on copies of {@link #definitionBuilder}. It also sets the parsed
   * parameters on the backing {@link AbstractExtensionObjectFactory}
   *
   * @return a list with the generated {@link ComponentBuildingDefinition}
   * @throws ConfigurationException if a parsing error occurs
   */
  public final List<ComponentBuildingDefinition> parse() throws ConfigurationException {
    Builder builder = definitionBuilder;
    builder = doParse(builder);

    AttributeDefinition parametersDefinition = fromFixedValue(new HashMap<>()).build();
    if (!parameters.isEmpty()) {
      KeyAttributeDefinitionPair[] attributeDefinitions = parameters.entrySet().stream()
          .map(entry -> newBuilder().withAttributeDefinition(entry.getValue().build()).withKey(entry.getKey()).build())
          .toArray(KeyAttributeDefinitionPair[]::new);
      parametersDefinition = fromMultipleDefinitions(attributeDefinitions).build();
    }

    builder = builder.withSetterParameterDefinition(PARAMETERS_FIELD_NAME, parametersDefinition);

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
          parseAttributeParameter(parameter);
        }


        @Override
        public void visitString(StringType stringType) {
          if (paramDsl.supportsChildDeclaration()) {
            parseFromTextExpression(parameter, paramDsl, () -> {
              Optional<QueryParameterModelProperty> query = parameter.getModelProperty(QueryParameterModelProperty.class);
              return value -> {
                ValueResolver<String> resolver =
                    valueResolverFactory.of(parameter.getName(), stringType, value, parameter.getDefaultValue(),
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
            if (!paramDsl.supportsTopLevelDeclaration() && paramDsl.supportsChildDeclaration()) {
              parsingContext.registerObjectType(paramDsl.getElementName(), paramDsl.getPrefix(), objectType);
            }
            parseAstParameter(parameter, paramDsl);
          } else {
            parseObject(parameter.getName(), parameter.getName(), objectType, parameter.getDefaultValue(),
                        parameter.getExpressionSupport(), parameter.isRequired(), acceptsReferences(parameter),
                        paramDsl, parameter.getModelProperties(), parameter.getAllowedStereotypes());
          }
        }

        @Override
        public void visitAnyType(AnyType anyType) {
          // In case that the anyType was assigned to a Java Object or a Java Serializable, handle it as a ObjectType as it
          // used to be done before we assigned AnyType to this kind of parameters. This is done because the Object or
          // Serializable can accept references.
          if (isInjectableType(anyType)) {

            if (!parseAsContent(anyType)) {
              parseAttributeParameter(parameter.getName(),
                                      parameter.getName(),
                                      parameter.getType(),
                                      parameter.getDefaultValue(),
                                      parameter.getExpressionSupport(),
                                      parameter.isRequired(),
                                      acceptsReferences(parameter),
                                      parameter.getModelProperties(),
                                      parameter.getAllowedStereotypes());
            }

          } else {
            defaultVisit(anyType);
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
            parseAstParameter(parameter, paramDsl);
            return true;
          }

          return false;
        }

      });
    });
  }

  private boolean isInjectableType(MetadataType type) {
    return type instanceof ObjectType || isReferableType(type);
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an open {@link ObjectType}
   *
   * @param parameter  a {@link ParameterModel}
   * @param objectType a {@link ObjectType}
   */
  protected void parseMapParameters(ParameterModel parameter, ObjectType objectType, DslElementSyntax paramDsl) {
    parseMapParameters(parameter.getName(), parameter.getName(), objectType, parameter.getDefaultValue(),
                       parameter.getExpressionSupport(), parameter.isRequired(), paramDsl, parameter.getModelProperties(),
                       parameter.getAllowedStereotypes());
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
                                    Set<ModelProperty> modelProperties,
                                    List<StereotypeModel> allowedStereotypes) {
    parseAttributeParameter(key, name, dictionaryType, defaultValue, expressionSupport, required, true, modelProperties,
                            allowedStereotypes);

    Class<? extends Map> mapType = getType(dictionaryType);
    if (ConcurrentMap.class.equals(mapType)) {
      mapType = ConcurrentHashMap.class;
    } else if (Map.class.equals(mapType)) {
      mapType = LinkedHashMap.class;
    }

    final MetadataType valueType = dictionaryType.getOpenRestriction().orElse(typeLoader.load(Object.class));
    final Class<?> valueClass = getType(valueType);
    final MetadataType keyType = typeLoader.load(String.class);
    final Class<?> keyClass = String.class;

    final String mapElementName = paramDsl.getElementName();

    addParameter(getChildKey(key), fromChildMapConfiguration(String.class, valueClass).withWrapperIdentifier(mapElementName)
        .withDefaultValue(defaultValue));

    addDefinition(definitionBuilder.withIdentifier(mapElementName).withTypeDefinition(fromType(mapType)).build());


    Optional<DslElementSyntax> mapValueChildDsl = paramDsl.getGeneric(valueType);
    if (!mapValueChildDsl.isPresent()) {
      return;
    }

    DslElementSyntax valueDsl = mapValueChildDsl.get();
    valueType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        String parameterName = paramDsl.getAttributeName();

        addDefinition(definitionBuilder
            .withIdentifier(valueDsl.getElementName())
            .withTypeDefinition(fromMapEntryType(keyClass, valueClass))
            .withKeyTypeConverter(getTypeConverter(parameterName, keyType, null, expressionSupport, true,
                                                   emptySet(), false))
            .withTypeConverter(getTypeConverter(parameterName, valueType, null, expressionSupport, true,
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
            new ObjectTypeParameterParser(definitionBuilder, objectType, getContextClassLoader(), dslResolver,
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
              addDefinition(definitionBuilder.withIdentifier(valueListGenericDsl.get().getElementName())
                  .withTypeDefinition(fromType(getType(metadataType)))
                  .withTypeConverter(getTypeConverter(parameterName, metadataType, getDefaultValue(metadataType),
                                                      getExpressionSupport(metadataType), false, emptySet(), true))
                  .build());
            }

            @Override
            protected void defaultVisit(MetadataType metadataType) {
              addDefinition(definitionBuilder.withIdentifier(valueListGenericDsl.get().getElementName())
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
                  fieldDsl.get(), emptySet(), emptyList());
      return;
    }

    final boolean isContent = isContent(parametersRole.getOrDefault(fieldName, BEHAVIOUR));
    fieldType.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        parseAttributeParameter(fieldName, fieldName, metadataType, defaultValue, expressionSupport, false, emptySet(),
                                objectField.getAnnotation(StereotypeTypeAnnotation.class)
                                    .map(StereotypeTypeAnnotation::getAllowedStereotypes)
                                    .orElse(emptyList()));
      }

      @Override
      public void visitString(StringType stringType) {
        if (fieldDsl.get().supportsChildDeclaration()) {
          String elementName = fieldDsl.get().getElementName();
          addParameter(fieldName, fromChildConfiguration(String.class).withWrapperIdentifier(elementName));
          addDefinition(definitionBuilder
              .withIdentifier(elementName)
              .withTypeDefinition(fromType(String.class))
              .withTypeConverter(getTypeConverter(elementName, stringType, defaultValue, expressionSupport, false, emptySet(),
                                                  acceptsReferences))
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
                               emptySet(), emptyList());
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
                               dsl, emptySet(), emptyList());
        } else {
          parseObject(fieldName, fieldName, objectType, defaultValue, expressionSupport, false, acceptsReferences,
                      dsl, emptySet(), emptyList());
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (!parseAsContent(isContent, arrayType)) {
          parseCollectionParameter(fieldName, fieldName, arrayType, defaultValue, expressionSupport, false, false, fieldDsl.get(),
                                   emptySet());
        }
      }

      private boolean parseAsContent(boolean isContent, MetadataType type) {
        if (isContent) {
          parseAstParameter(fieldName, fieldName, type, fieldDsl.get(), defaultValue, expressionSupport, false, emptySet(),
                            objectField.getAnnotation(StereotypeTypeAnnotation.class)
                                .map(StereotypeTypeAnnotation::getAllowedStereotypes)
                                .orElse(emptyList()),
                            isContent);

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
    parseCollectionParameter(parameter.getName(), parameter.getName(), arrayType, parameter.getDefaultValue(),
                             parameter.getExpressionSupport(), parameter.isRequired(),
                             parameter.getDslConfiguration().allowsReferences(), parameterDsl,
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
                                          ExpressionSupport expressionSupport, boolean required, boolean acceptsReferences,
                                          DslElementSyntax parameterDsl,
                                          Set<ModelProperty> modelProperties) {

    parseAttributeParameter(key, name, arrayType, defaultValue, expressionSupport, required, acceptsReferences, modelProperties,
                            emptyList());

    Class<?> collectionType = ExtensionMetadataTypeUtils.getType(arrayType).orElse(null);

    if (Set.class.equals(collectionType)) {
      collectionType = HashSet.class;
    } else if (Collection.class.equals(collectionType) || Iterable.class.equals(collectionType) || collectionType == null) {
      collectionType = List.class;
    }

    final String collectionElementName = parameterDsl.getElementName();
    addParameter(getChildKey(key), fromChildConfiguration(collectionType).withWrapperIdentifier(collectionElementName));
    addDefinition(definitionBuilder.withIdentifier(collectionElementName).withTypeDefinition(fromType(collectionType))
        .build());


    Optional<DslElementSyntax> collectionItemDsl = parameterDsl.getGeneric(arrayType.getType());
    if (parameterDsl.supportsChildDeclaration() && collectionItemDsl.isPresent()) {
      String itemIdentifier = collectionItemDsl.get().getElementName();
      String itemNamespace = collectionItemDsl.get().getPrefix();

      arrayType.getType().accept(new BasicTypeMetadataVisitor() {

        private void addBasicTypeDefinition(MetadataType metadataType) {
          TypeConverter typeConverter = getTypeConverter(name, metadataType, getDefaultValue(metadataType).orElse(null),
                                                         getExpressionSupport(metadataType), false, emptySet(), true);

          Builder itemDefinitionBuilder = definitionBuilder.withIdentifier(itemIdentifier).withNamespace(itemNamespace)
              .withTypeDefinition(fromType(ExtensionMetadataTypeUtils.getType(metadataType).orElse(Object.class)))
              .withTypeConverter(typeConverter);
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
              new ObjectTypeParameterParser(definitionBuilder, objectType, getContextClassLoader(), dslResolver,
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
    return currentThread().getContextClassLoader();
  }

  protected void parseFromTextExpression(ParameterModel parameter, DslElementSyntax dsl, Supplier<TypeConverter> typeConverter) {
    parseFromTextExpression(parameter.getName(), dsl, typeConverter);
  }

  protected void parseFromTextExpression(String key, DslElementSyntax paramDsl, Supplier<TypeConverter> typeConverter) {
    addParameter(getChildKey(key), fromSimpleParameter(key, typeConverter.get()));

    addDefinition(definitionBuilder
        .withIdentifier(paramDsl.getElementName())
        .withTypeDefinition(fromType(String.class))
        .withTypeConverter(typeConverter.get())
        .build());
  }

  /**
   * Registers a definition for parsing the given {@code parameterModel} as an element attribute
   *
   * @param parameterModel a {@link ParameterModel}
   * @return an {@link AttributeDefinition.Builder}
   */
  protected AttributeDefinition.Builder parseAttributeParameter(ParameterModel parameterModel) {
    return parseAttributeParameter(parameterModel.getName(), parameterModel.getName(), parameterModel.getType(),
                                   parameterModel.getDefaultValue(), parameterModel.getExpressionSupport(),
                                   parameterModel.isRequired(),
                                   !parameterModel.getAllowedStereotypes().isEmpty(),
                                   parameterModel.getModelProperties(),
                                   parameterModel.getAllowedStereotypes());
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
                                                                Set<ModelProperty> modelProperties,
                                                                List<StereotypeModel> allowedStereotypes) {
    return parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required, !allowedStereotypes.isEmpty(),
                                   modelProperties, allowedStereotypes);
  }

  private AttributeDefinition.Builder parseAttributeParameter(String key, String name, MetadataType type, Object defaultValue,
                                                              ExpressionSupport expressionSupport, boolean required,
                                                              boolean acceptsReferences, Set<ModelProperty> modelProperties,
                                                              List<StereotypeModel> allowedStereotypes) {
    AttributeDefinition.Builder definitionBuilder;

    TypeConverter typeConverter = getTypeConverter(name, type, defaultValue, expressionSupport, required,
                                                   modelProperties, acceptsReferences);

    if (acceptsReferences && type instanceof StringType && !allowedStereotypes.isEmpty()) {
      definitionBuilder = fromSoftReferenceSimpleParameter(name);
    } else if (acceptsReferences && expressionSupport == NOT_SUPPORTED && type instanceof ObjectType) {
      definitionBuilder = fromSimpleReferenceParameter(key);
    } else if (acceptsReferences && type instanceof ObjectType) {
      definitionBuilder = fromSimpleReferenceParameter(key, typeConverter);
    } else {
      definitionBuilder = fromSimpleParameter(name, typeConverter);
    }

    definitionBuilder.withDefaultValue(defaultValue);
    addParameter(key, definitionBuilder);

    return definitionBuilder;
  }

  protected void parseAstParameter(String key, String name, MetadataType type, DslElementSyntax dsl,
                                   Object defaultValue,
                                   ExpressionSupport expressionSupport, boolean required,
                                   Set<ModelProperty> modelProperties,
                                   List<StereotypeModel> allowedStereotypes, boolean content) {
    if (content) {
      TypeConverter typeConverter = getTypeConverter(name, type, defaultValue, expressionSupport, required,
                                                     modelProperties, !allowedStereotypes.isEmpty());
      parseFromTextExpression(name, dsl, () -> typeConverter);
    } else {
      parseObjectParameter(key, name, (ObjectType) type,
                           defaultValue, expressionSupport, required,
                           !allowedStereotypes.isEmpty(), dsl, modelProperties,
                           allowedStereotypes);
    }
  }

  /**
   * Registers a definition for a {@link ParameterModel} which represents an {@link ObjectType}
   *
   * @param parameterModel a {@link ParameterModel}
   */
  protected void parseAstParameter(ParameterModel parameterModel, DslElementSyntax paramDsl) {
    if (isContent(parameterModel)) {

      TypeConverter typeConverter = getTypeConverter(parameterModel.getName(),
                                                     parameterModel.getType(),
                                                     parameterModel.getDefaultValue(),
                                                     parameterModel.getExpressionSupport(),
                                                     parameterModel.isRequired(),
                                                     parameterModel.getModelProperties(),
                                                     acceptsReferences(parameterModel));

      parseFromTextExpression(parameterModel, paramDsl, () -> typeConverter);
    } else {
      parseObjectParameter(parameterModel.getName(), parameterModel.getName(), (ObjectType) parameterModel.getType(),
                           parameterModel.getDefaultValue(), parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                           acceptsReferences(parameterModel), paramDsl, parameterModel.getModelProperties(),
                           parameterModel.getAllowedStereotypes());
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
                                      Set<ModelProperty> modelProperties,
                                      List<StereotypeModel> allowedStereotypes) {

    parseObject(key, name, type, defaultValue, expressionSupport, required, acceptsReferences, elementDsl,
                modelProperties, allowedStereotypes);

    final String elementNamespace = elementDsl.getPrefix();
    final String elementName = elementDsl.getElementName();

    if (elementDsl.supportsChildDeclaration() && !elementDsl.isWrapped() &&
        modelProperties.stream().noneMatch(m -> m.getName().equals(InfrastructureParameterModelProperty.NAME))) {
      try {
        new ObjectTypeParameterParser(definitionBuilder, elementName, elementNamespace, type, getContextClassLoader(),
                                      dslResolver, parsingContext).parse()
                                          .forEach(this::addDefinition);
      } catch (Exception e) {
        throw new MuleRuntimeException(new ConfigurationException(e));
      }
    }
  }

  protected void parseObject(String key, String name, ObjectType type, Object defaultValue, ExpressionSupport expressionSupport,
                             boolean required, boolean acceptsReferences, DslElementSyntax elementDsl,
                             Set<ModelProperty> modelProperties, List<StereotypeModel> allowedStereotypes) {
    parseAttributeParameter(key, name, type, defaultValue, expressionSupport, required, acceptsReferences,
                            modelProperties, allowedStereotypes);

    ObjectParsingDelegate delegate = (ObjectParsingDelegate) locateParsingDelegate(objectParsingDelegates, type)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find a parsing delegate for type "
            + getType(type).getName())));

    addParameter(getChildKey(key), delegate.parse(name, type, elementDsl));
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

    addDefinition(definitionBuilder.withIdentifier(processorElementName)
        .withTypeDefinition(fromType(ProcessorChainValueResolver.class))
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Processor.class).build())
        .build());
  }

  private void parseRoute(NestedRouteModel routeModel) {
    DslElementSyntax routeDsl = dslResolver.resolve(routeModel);

    Class<?> type = getImplementingType(routeModel)
        .orElseThrow(() -> new IllegalStateException("Missing route information"));

    MetadataType metadataType = typeLoader.load(type);
    addParameter(getChildKey(routeModel.getName()),
                 new DefaultObjectParsingDelegate().parse(routeModel.getName(), (ObjectType) metadataType, routeDsl));

    try {
      new RouteComponentParser(definitionBuilder, routeModel, metadataType, getContextClassLoader(), routeDsl,
                               dslResolver, parsingContext).parse()
                                   .forEach(this::addDefinition);
    } catch (Exception e) {
      throw new MuleRuntimeException(new ConfigurationException(e));
    }
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
      new TypedInlineParameterGroupParser(definitionBuilder, group, descriptor, getContextClassLoader(),
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
      new AnonymousInlineParameterGroupParser(definitionBuilder, group, getContextClassLoader(), dslElementSyntax,
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

  private TypeConverter getTypeConverter(String name, MetadataType type, Object defaultValue, ExpressionSupport expressionSupport,
                                         boolean required, Set<ModelProperty> modelProperties, boolean acceptsReferences) {

    ClassLoader extensionClassLoader = getContextClassLoader();
    TypeConverter typeConverter = value -> {
      Thread thread = Thread.currentThread();
      ClassLoader currentClassLoader = thread.getContextClassLoader();
      CompositeClassLoader compositeClassLoader = from(extensionClassLoader, currentClassLoader);
      setContextClassLoader(thread, currentClassLoader, compositeClassLoader);
      try {
        return valueResolverFactory.of(name, type, value, defaultValue, expressionSupport, required, modelProperties,
                                       acceptsReferences);
      } finally {
        setContextClassLoader(thread, compositeClassLoader, currentClassLoader);
      }
    };

    return typeConverter;
  }
}
