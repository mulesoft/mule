/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.disjunction;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_REF_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.internal.model.properties.PropertiesResolverUtils.createConfigurationAttributeResolver;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.util.NameUtils.pluralize;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.util.NameValidationUtil.verifyStringDoesNotContainsReservedCharacters;
import static org.mule.runtime.module.extension.internal.runtime.exception.ErrorMappingUtils.forEachErrorMappingDo;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.builder.ArtifactAstBuilder;
import org.mule.runtime.ast.api.builder.ComponentAstBuilder;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.dsl.model.ComponentModelReader;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.privileged.extension.SingletonModelProperty;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

/**
 * An {@code ApplicationModel} holds a representation of all the artifact configuration using an abstract model to represent any
 * configuration option.
 * <p/>
 * This model is represented by a set of {@link ComponentAst}a. Each {@code ComponentAst} holds a piece of configuration and may
 * have children {@code ComponentAst}s as defined in the artifact configuration.
 * <p/>
 * Once the set of {@code ComponentAst}s gets created from the application {@link ConfigFile}s the {@code ApplicationModel}
 * executes a set of common validations dictated by the configuration semantics.
 *
 * @since 4.0
 */
public class ApplicationModel implements ArtifactAst {

  private static final Logger LOGGER = getLogger(ApplicationModel.class);

  // TODO MULE-9692 move this logic elsewhere. This are here just for the language rules and those should be processed elsewhere.
  public static final String ERROR_MAPPING = "error-mapping";
  public static final String ON_ERROR = "on-error";
  public static final String MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE = "maxRedeliveryAttempts";
  public static final String WHEN_CHOICE_ES_ATTRIBUTE = "when";
  public static final String TYPE_ES_ATTRIBUTE = "type";
  public static final String PROPERTY_ELEMENT = "property";
  public static final String NAME_ATTRIBUTE = "name";
  public static final String REFERENCE_ATTRIBUTE = "ref";
  public static final String VALUE_ATTRIBUTE = "value";
  public static final String DESCRIPTION_ELEMENT = "description";
  public static final String PROPERTIES_ELEMENT = "properties";

  public static final String REDELIVERY_POLICY_ELEMENT = "redelivery-policy";
  // TODO MULE-9638 Remove once all bean definitions parsers have been migrated
  public static final String TEST_NAMESPACE = "test";
  public static final String DOC_NAMESPACE = "doc";
  public static final String GLOBAL_PROPERTY = "global-property";
  public static final String SECURITY_MANAGER = "security-manager";
  public static final String OBJECT_ELEMENT = "object";


  public static final ComponentIdentifier ERROR_MAPPING_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ERROR_MAPPING).build();
  public static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  public static final ComponentIdentifier MULE_PROPERTY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(PROPERTY_ELEMENT).build();
  public static final ComponentIdentifier MULE_PROPERTIES_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(PROPERTIES_ELEMENT).build();
  public static final ComponentIdentifier ANNOTATIONS_ELEMENT_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ANNOTATIONS_PROPERTY_NAME).build();
  public static final ComponentIdentifier DOC_DESCRIPTION_IDENTIFIER =
      builder().namespace(DOC_NAMESPACE).name(DESCRIPTION_ELEMENT).build();
  public static final ComponentIdentifier DESCRIPTION_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(DESCRIPTION_ELEMENT).build();
  public static final ComponentIdentifier OBJECT_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(OBJECT_ELEMENT).build();
  public static final ComponentIdentifier REDELIVERY_POLICY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(REDELIVERY_POLICY_ELEMENT).build();
  public static final ComponentIdentifier GLOBAL_PROPERTY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(GLOBAL_PROPERTY).build();
  public static final ComponentIdentifier SECURITY_MANAGER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SECURITY_MANAGER).build();

  public static final String CLASS_ATTRIBUTE = "class";

  private static ImmutableSet<ComponentIdentifier> ignoredNameValidationComponentList =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("alias").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("password-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-security-provider")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT)
              .name("secret-key-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("import").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("security-manager").build())
          // TODO MULE-18366 Remove these entries from test namespace
          .add(builder().namespace(TEST_NAMESPACE).name("queue").build())
          .add(builder().namespace(TEST_NAMESPACE).name("invocation-counter").build())
          .build();

  private ArtifactAst ast;
  private final ArtifactAst originalAst;
  private final PropertiesResolverConfigurationProperties configurationProperties;
  private final Map<String, ComponentAst> namedTopLevelComponentModels = new HashMap<>();

  /**
   * Creates an {code ApplicationModel} from a {@link ArtifactConfig}.
   * <p/>
   * A set of validations are applied that may make creation fail.
   *
   * @param artifactConfig the mule artifact configuration content.
   * @param artifactDeclaration an {@link ArtifactDeclaration}
   * @param deploymentProperties values for replacement of properties in the DSL
   * @param extensionModels Set of {@link ExtensionModel extensionModels} that will be used to type componentModels
   * @param parentConfigurationProperties the {@link ConfigurationProperties} of the parent artifact. For instance, application
   *        will receive the domain resolver.
   * @param componentBuildingDefinitionRegistry an optional {@link ComponentBuildingDefinitionRegistry} used to correlate items in
   *        this model to their definitions expanded) false implies the mule is being created from a tooling perspective.
   * @param externalResourceProvider the provider for configuration properties files and ${file::name.txt} placeholders
   * @throws Exception when the application configuration has semantic errors.
   */
  public ApplicationModel(ArtifactConfig artifactConfig, ArtifactDeclaration artifactDeclaration,
                          Set<ExtensionModel> extensionModels,
                          Map<String, String> deploymentProperties,
                          Optional<ConfigurationProperties> parentConfigurationProperties,
                          ResourceProvider externalResourceProvider) {
    // this basic resolver is needed to resolve the properties used in names to properly generate the structure of the AST.
    final PropertiesResolverConfigurationProperties baseConfigurationAttributeResolver =
        createConfigurationAttributeResolver(parentConfigurationProperties,
                                             deploymentProperties, externalResourceProvider);
    final ArtifactAstBuilder astBuilder =
        ArtifactAstBuilder.builder(extensionModels, baseConfigurationAttributeResolver.getConfigurationPropertiesResolver());

    convertConfigFileToComponentModel(artifactConfig, astBuilder);
    convertArtifactDeclarationToComponentModel(extensionModels, artifactDeclaration, astBuilder);
    this.originalAst = astBuilder.build();

    this.configurationProperties = createConfigurationAttributeResolver(originalAst, parentConfigurationProperties,
                                                                        deploymentProperties, externalResourceProvider);
    try {
      initialiseIfNeeded(configurationProperties.getConfigurationPropertiesResolver());
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }

    this.originalAst
        .updatePropertiesResolver(configurationProperties.getConfigurationPropertiesResolver());

    this.ast = originalAst;
    indexComponentModels(originalAst);

    validateModel();
  }

  private void indexComponentModels(ArtifactAst originalAst) {
    originalAst.topLevelComponentsStream()
        .forEach(componentModel -> componentModel.getComponentId()
            .ifPresent(name -> namedTopLevelComponentModels.put(name, componentModel)));
  }

  /**
   * Preprocesses the ArtifactAst so that it can be deployed to runtime.
   *
   * @param extensionModels
   */
  public void prepareAstForRuntime(Set<ExtensionModel> extensionModels) {
    ast = processSourcesRedeliveryPolicy(ast);
    ast = doXmlSdk1MacroExpansion(ast, extensionModels);
  }

  /**
   * We force the current instance of {@link ApplicationModel} to be highly cohesive with {@link MacroExpansionModulesModel} as
   * it's responsibility of this object to properly initialize and expand every global element/operation into the concrete set of
   * message processors
   *
   * @param extensionModels Set of {@link ExtensionModel extensionModels} that will be used to check if the element has to be
   *        expanded.
   * @param postProcess a closure to be executed after the macroexpansion of an extension.
   */
  private ArtifactAst doXmlSdk1MacroExpansion(ArtifactAst ast, Set<ExtensionModel> extensionModels) {
    return new MacroExpansionModulesModel(ast, extensionModels).expand();
  }

  public void close() {
    disposeIfNeeded(configurationProperties.getConfigurationPropertiesResolver(), LOGGER);
  }

  /**
   * Process from any message source the redelivery-policy to make it part of the final pipeline.
   */
  private ArtifactAst processSourcesRedeliveryPolicy(ArtifactAst ast) {
    return copyRecursively(ast, flow -> {

      if (FLOW_IDENTIFIER.equals(flow.getIdentifier())) {
        return flow.directChildrenStream().findFirst()
            .filter(comp -> comp.getModel(SourceModel.class).isPresent())
            .map(source -> source.directChildrenStream()
                .filter(childComponent -> REDELIVERY_POLICY_IDENTIFIER.equals(childComponent.getIdentifier()))
                .findAny()
                .map(redeliveryPolicy -> transformFlowWithRedeliveryPolicy(flow, source, redeliveryPolicy))
                .orElse(flow))
            .orElse(flow);
      }

      return flow;

    });
  }

  private ComponentAst transformFlowWithRedeliveryPolicy(ComponentAst flow, ComponentAst source, ComponentAst redeliveryPolicy) {
    final List<ComponentAst> newFlowChildren = new ArrayList<>();

    newFlowChildren.add(new BaseComponentAstDecorator(source) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        // The transformed source is the same with the redelivery-policy removed...
        return super.directChildrenStream()
            .filter(sourceChild -> sourceChild != redeliveryPolicy);

      }
    });
    newFlowChildren.add(new BaseComponentAstDecorator(redeliveryPolicy) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        // The redelivery-policy is added to the flow wrapping the flow processors
        return flow.directChildrenStream()
            .filter(comp -> !comp.getModel(SourceModel.class).isPresent()
                && !ERROR_HANDLER_IDENTIFIER.equals(comp.getIdentifier()));
      }

    });

    // The error handlers of the flow are kept
    flow.directChildrenStream()
        .filter(comp -> ERROR_HANDLER_IDENTIFIER.equals(comp.getIdentifier()))
        .forEach(newFlowChildren::add);

    return new BaseComponentAstDecorator(flow) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        return newFlowChildren.stream();
      }

    };
  }

  private void convertArtifactDeclarationToComponentModel(Set<ExtensionModel> extensionModels,
                                                          ArtifactDeclaration artifactDeclaration,
                                                          ArtifactAstBuilder astBuilder) {
    if (artifactDeclaration != null && !extensionModels.isEmpty()) {
      ExtensionModel muleModel = MuleExtensionModelProvider.getExtensionModel();
      if (!extensionModels.contains(muleModel)) {
        extensionModels = new HashSet<>(extensionModels);
        extensionModels.add(muleModel);
      }

      DslElementModelFactory elementFactory = DslElementModelFactory.getDefault(DslResolvingContext.getDefault(extensionModels));

      artifactDeclaration.getGlobalElements().stream()
          .map(e -> elementFactory.create((ElementDeclaration) e))
          .filter(Optional::isPresent)
          .map(e -> e.get().getConfiguration())
          .forEach(config -> config
              .ifPresent(c -> convertComponentConfiguration(c, astBuilder.addTopLevelComponent())));
    }
  }

  private void convertComponentConfiguration(ComponentConfiguration componentConfiguration,
                                             ComponentAstBuilder componentAstBuilder) {
    componentAstBuilder
        .withIdentifier(componentConfiguration.getIdentifier())
        .withMetadata(ComponentMetadataAst.builder().build());
    for (Map.Entry<String, String> parameter : componentConfiguration.getParameters().entrySet()) {
      componentAstBuilder.withRawParameter(parameter.getKey(), parameter.getValue());
    }
    componentConfiguration.getValue().ifPresent(value -> componentAstBuilder.withRawParameter(BODY_RAW_PARAM_NAME, value));
    for (ComponentConfiguration childComponentConfiguration : componentConfiguration.getNestedComponents()) {
      convertComponentConfiguration(childComponentConfiguration, componentAstBuilder.addChildComponent());
    }
  }


  public Optional<ComponentAst> findComponentDefinitionModel(ComponentIdentifier componentIdentifier) {
    return topLevelComponentsStream()
        .filter(componentModel -> componentModel.getIdentifier().equals(componentIdentifier)).findFirst();
  }

  private void convertConfigFileToComponentModel(ArtifactConfig artifactConfig, ArtifactAstBuilder astBuilder) {
    ComponentModelReader componentModelReader = new ComponentModelReader();

    List<ConfigFile> configFiles = artifactConfig.getConfigFiles();
    configFiles.stream()
        .forEach(configFile -> configFile.getConfigLines().get(0).getChildren()
            .forEach(topLevelConfigLine -> componentModelReader
                .extractComponentDefinitionModel(topLevelConfigLine, configFile.getFilename(),
                                                 astBuilder.addTopLevelComponent())));
  }

  private void validateModel() {
    // TODO MULE-18318 (AST) all this validations will be moved to an entity that does the validation and allows to aggregate all
    // validations instead of failing fast.
    validateSingletonsAreNotRepeated();
    validateSingletonsAreNotRepeatedPerFile();
    validateNameIsNotRepeated();
    validateNameHasValidCharacters();
    validateFlowRefPointsToExistingFlow();
    validateErrorMappings();
    validateErrorHandlerStructure();
    // TODO MULE-17711 (AST) re-enable (and possibly refactor) this validation
    // validateParameterAndChildForSameAttributeAreNotDefinedTogether();
    validateNamedTopLevelElementsHaveName();
    validateNoExpressionsInNoExpressionsSupportedParams();
  }

  private void validateFlowRefPointsToExistingFlow() {
    recursiveStream()
        .filter(componentModel -> componentModel.getIdentifier().equals(FLOW_REF_IDENTIFIER))
        .forEach(componentModel -> {
          componentModel.getParameter("name").getValue().applyRight(nameAttribute -> {
            // Need to check this, since the name field is a reference according to the extModel and the AST assumes there are no
            // expressions there.
            if (!((String) nameAttribute).startsWith(DEFAULT_EXPRESSION_PREFIX)) {
              findTopLevelNamedComponent((String) nameAttribute)
                  .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("flow-ref at %s:%s is pointing to %s which does not exist",
                                                                                  componentModel.getMetadata().getFileName()
                                                                                      .orElse("unknown"),
                                                                                  componentModel.getMetadata().getStartLine()
                                                                                      .orElse(-1),
                                                                                  nameAttribute)));
            }
          });
        });
  }

  private void validateParameterAndChildForSameAttributeAreNotDefinedTogether() {
    recursiveStream().forEach(componentModel -> componentModel.directChildrenStream()
        .forEach(child -> {
          final String paramName = toCamelCase(child.getIdentifier().getName(), "-");
          final String singularParamName = pluralize(paramName);

          if (componentModel.getRawParameterValue(paramName).isPresent()
              || componentModel.getRawParameterValue(singularParamName).isPresent()) {
            throw new MuleRuntimeException(createStaticMessage(
                                                               format("Component %s has a child element %s which is used for the same purpose of the configuration parameter %s. "
                                                                   + "Only one must be used.", componentModel.getIdentifier(),
                                                                      child.getIdentifier(),
                                                                      singularParamName)));
          }
        }));
  }

  private void validateSingletonsAreNotRepeated() {
    Map<ComponentIdentifier, ComponentAst> existingSingletonsByIdentifier = new HashMap<>();

    topLevelComponentsStream().forEach(componentModel -> {
      if (componentModel.getModel(EnrichableModel.class)
          .flatMap(enrchModel -> enrchModel.getModelProperty(SingletonModelProperty.class).map(smp -> !smp.isAppliesToFile()))
          .orElse(false)) {
        ComponentIdentifier singletonIdentifier = componentModel.getIdentifier();
        if (existingSingletonsByIdentifier.containsKey(singletonIdentifier)) {
          ComponentAst otherComponentModel = existingSingletonsByIdentifier.get(singletonIdentifier);
          if (componentModel.getMetadata().getFileName().isPresent()
              && componentModel.getMetadata().getStartLine().isPresent()
              && otherComponentModel.getMetadata().getFileName().isPresent()
              && otherComponentModel.getMetadata().getStartLine().isPresent()) {
            throw new MuleRuntimeException(createStaticMessage("The configuration element [%s] can only appear once, but was present in both [%s:%d] and [%s:%d]",
                                                               componentModel.getIdentifier(),
                                                               otherComponentModel.getMetadata().getFileName().get(),
                                                               otherComponentModel.getMetadata().getStartLine().getAsInt(),
                                                               componentModel.getMetadata().getFileName().get(),
                                                               componentModel.getMetadata().getStartLine().getAsInt()));
          } else {
            throw new MuleRuntimeException(createStaticMessage("The configuration element [%s] can only appear once, but was present multiple times",
                                                               componentModel.getIdentifier()));
          }
        }
        existingSingletonsByIdentifier.put(singletonIdentifier, componentModel);
      }
    });
  }

  private void validateSingletonsAreNotRepeatedPerFile() {
    Map<String, Map<ComponentIdentifier, ComponentAst>> existingSingletonsByIdentifierByFileName = new HashMap<>();

    topLevelComponentsStream().forEach(componentModel -> {
      if (componentModel.getModel(EnrichableModel.class)
          .flatMap(enrchModel -> enrchModel.getModelProperty(SingletonModelProperty.class)
              .map(SingletonModelProperty::isAppliesToFile))
          .orElse(false)) {

        componentModel.getMetadata().getFileName()
            .ifPresent(fileName -> {
              Map<ComponentIdentifier, ComponentAst> existingSingletonsByIdentifier =
                  existingSingletonsByIdentifierByFileName.computeIfAbsent(fileName, k -> new HashMap<>());

              ComponentIdentifier singletonIdentifier = componentModel.getIdentifier();
              if (existingSingletonsByIdentifier.containsKey(singletonIdentifier)) {
                ComponentAst otherComponentModel = existingSingletonsByIdentifier.get(singletonIdentifier);
                if (componentModel.getMetadata().getFileName().isPresent()
                    && componentModel.getMetadata().getStartLine().isPresent()
                    && otherComponentModel.getMetadata().getFileName().isPresent()
                    && otherComponentModel.getMetadata().getStartLine().isPresent()) {
                  throw new MuleRuntimeException(createStaticMessage("The configuration element [%s] can only appear once, but was present in both [%s:%d] and [%s:%d]",
                                                                     componentModel.getIdentifier(),
                                                                     otherComponentModel.getMetadata().getFileName().get(),
                                                                     otherComponentModel.getMetadata().getStartLine().getAsInt(),
                                                                     componentModel.getMetadata().getFileName().get(),
                                                                     componentModel.getMetadata().getStartLine().getAsInt()));
                } else {
                  throw new MuleRuntimeException(createStaticMessage("The configuration element [%s] can only appear once, but was present multiple times",
                                                                     componentModel.getIdentifier()));
                }
              }
              existingSingletonsByIdentifier.put(singletonIdentifier, componentModel);
            });
      }
    });
  }

  private void validateNameIsNotRepeated() {
    Map<String, ComponentAst> existingObjectsWithName = new HashMap<>();
    topLevelComponentsStream()
        .filter(componentModel -> !ignoredNameValidationComponentList.contains(componentModel.getIdentifier()))
        .filter(componentModel -> !componentModel.getModel(HasStereotypeModel.class)
            .map(sm -> sm.getStereotype().isAssignableTo(APP_CONFIG))
            .orElse(false))
        .forEach(componentModel -> componentModel.getComponentId()
            .ifPresent(nameAttributeValue -> {
              if (existingObjectsWithName.containsKey(nameAttributeValue)) {
                throw new MuleRuntimeException(createStaticMessage("Two configuration elements have been defined with the same global name. Global name [%s] must be unique. Clashing components are %s and %s",
                                                                   nameAttributeValue,
                                                                   existingObjectsWithName.get(nameAttributeValue)
                                                                       .getIdentifier(),
                                                                   componentModel.getIdentifier()));
              }
              existingObjectsWithName.put(nameAttributeValue, componentModel);
            }));
  }

  private void validateNameHasValidCharacters() {
    topLevelComponentsStream().forEach(componentModel -> componentModel.getComponentId()
        .ifPresent(nameAttributeValue -> {
          try {
            verifyStringDoesNotContainsReservedCharacters(nameAttributeValue);
          } catch (IllegalArgumentException e) {
            throw new MuleRuntimeException(createStaticMessage(format("Invalid global element name '%s' in %s:%s. Problem is: %s",
                                                                      nameAttributeValue,
                                                                      componentModel.getMetadata().getFileName()
                                                                          .orElse("unknown"),
                                                                      componentModel.getMetadata().getStartLine().orElse(-1),
                                                                      e.getMessage())));
          }
        }));
  }

  private void validateErrorMappings() {
    recursiveStream().forEach(componentModel -> forEachErrorMappingDo(componentModel, mappings -> {
      List<ErrorMapping> anyMappings = mappings.stream()
          .filter(this::isErrorMappingWithSourceAny)
          .collect(toList());
      if (anyMappings.size() > 1) {
        throw new MuleRuntimeException(createStaticMessage("Only one mapping for 'ANY' or an empty source type is allowed."));
      } else if (anyMappings.size() == 1 && !isErrorMappingWithSourceAny(mappings.get(mappings.size() - 1))) {
        throw new MuleRuntimeException(createStaticMessage("Only the last error mapping can have 'ANY' or an empty source type."));
      }
      List<String> sources = mappings.stream()
          .map(ErrorMapping::getSource)
          .collect(toList());
      List<String> distinctSources = sources.stream()
          .distinct()
          .collect(toList());
      if (sources.size() != distinctSources.size()) {
        throw new MuleRuntimeException(createStaticMessage(format("Repeated source types are not allowed. Offending types are '%s'.",
                                                                  on("', '")
                                                                      .join(disjunction(sources, distinctSources)))));
      }
    }));
  }

  private boolean isErrorMappingWithSourceAny(ErrorMapping model) {
    return ANY_IDENTIFIER.equals(model.getSource());
  }

  private void validateErrorHandlerStructure() {
    recursiveStream().forEach(component -> {
      if (component.getIdentifier().equals(ERROR_HANDLER_IDENTIFIER)) {
        validateRefOrOnErrorsExclusiveness(component);
        validateOnErrorsHaveTypeOrWhenAttribute(component);
        validateNoMoreThanOneOnErrorPropagateWithRedelivery(component);
      }
    });
  }

  private void validateRefOrOnErrorsExclusiveness(ComponentAst component) {
    if (component.getRawParameterValue(REFERENCE_ATTRIBUTE).isPresent()
        && component.directChildrenStream().count() > 0) {
      throw new MuleRuntimeException(createStaticMessage("A reference error-handler cannot have on-errors."));
    }
  }

  private void validateNoMoreThanOneOnErrorPropagateWithRedelivery(ComponentAst component) {
    if (component.directChildrenStream()
        .filter(exceptionStrategyComponent -> exceptionStrategyComponent
            .getRawParameterValue(MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE).isPresent())
        .count() > 1) {
      throw new MuleRuntimeException(createStaticMessage("Only one on-error-propagate within a error-handler can handle message redelivery. Remove one of the maxRedeliveryAttempts attributes"));
    }
  }

  private void validateOnErrorsHaveTypeOrWhenAttribute(ComponentAst component) {
    final long count = component.directChildrenStream().count();
    if (count > 0) {
      component.directChildrenStream()
          // last error handler should be the catch all, so that one is not validated
          .limit(count - 1)
          .forEach(innerComponent -> {
            ComponentAst componentModel = getOnErrorComponentModel(innerComponent);
            if (!componentModel.getRawParameterValue(WHEN_CHOICE_ES_ATTRIBUTE).isPresent()
                && !componentModel.getRawParameterValue(TYPE_ES_ATTRIBUTE).isPresent()) {
              throw new MuleRuntimeException(createStaticMessage(
                                                                 "Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute."));
            }
          });
    }
  }

  private ComponentAst getOnErrorComponentModel(ComponentAst onErrorModel) {
    if (ON_ERROR_IDENTIFIER.equals(onErrorModel.getIdentifier())) {
      return onErrorModel.getRawParameterValue(REFERENCE_ATTRIBUTE)
          .map(sharedOnErrorName -> findTopLevelNamedComponent(sharedOnErrorName)
              .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("Could not find on-error reference named '%s'",
                                                                                     sharedOnErrorName)))))
          .orElse(onErrorModel);
    } else {
      return onErrorModel;
    }
  }

  private void validateNamedTopLevelElementsHaveName() {
    topLevelComponentsStream().forEach(topLevelComponent -> {
      if (topLevelComponent.getComponentId().isPresent()) {
        // We have a name, good to go!
        return;
      }
      if (!topLevelComponent.getModel(Object.class).isPresent()) {
        // Skip the validation if there is no model available. This situation happens in some test cases.
        return;
      }
      if (topLevelComponent.getModel(HasStereotypeModel.class)
          .map(sm -> sm.getStereotype().isAssignableTo(APP_CONFIG))
          .orElse(false)) {
        // APP_CONFIGs need not be referenced by name, so this exception is ok
        // Not making this exception would break backwards compatibility in spring:security-manager
        return;
      }

      if (!topLevelComponent.getModel(ConstructModel.class).isPresent()
          || topLevelComponent.getModel(ParameterizedModel.class)
              .map(pmzd -> pmzd.getAllParameterModels().stream()
                  .anyMatch(ParameterModel::isComponentId))
              .orElse(false)) {
        final ComponentIdentifier identifier = topLevelComponent.getIdentifier();
        throw new MuleRuntimeException(createStaticMessage(format("Global element %s:%s does not provide a name attribute.",
                                                                  identifier.getNamespace(), identifier.getName())));
      }
    });
  }

  private void validateNoExpressionsInNoExpressionsSupportedParams() {
    recursiveStream()
        .filter(component -> component.getModel(ParameterizedModel.class).isPresent())
        .forEach(component -> component.getParameters()
            .forEach(param -> param.getValue()
                .applyLeft(expr -> {
                  if (NOT_SUPPORTED.equals(param.getModel().getExpressionSupport())
                      && !param.getModel().getType().getAnnotation(LiteralTypeAnnotation.class).isPresent()) {
                    throw new MuleRuntimeException(createStaticMessage(format("An expression value was given for parameter '%s' but it doesn't support expressions",
                                                                              param.getModel().getName())));
                  }
                })));
  }

  /**
   * Find a named component configuration.
   *
   * @param name the expected value for the name attribute configuration.
   * @return the component if present, if not, an empty {@link Optional}
   */
  public Optional<ComponentAst> findTopLevelNamedComponent(String name) {
    return ofNullable(namedTopLevelComponentModels.getOrDefault(name, null));
  }

  /**
   * @return the attributes resolver for this artifact.
   */
  public ConfigurationProperties getConfigurationProperties() {
    return configurationProperties;
  }

  @Override
  public Stream<ComponentAst> recursiveStream(AstTraversalDirection direction) {
    return ast.recursiveStream(direction);
  }

  @Override
  public Spliterator<ComponentAst> recursiveSpliterator(AstTraversalDirection direction) {
    return ast.recursiveSpliterator(direction);
  }

  @Override
  public Stream<ComponentAst> topLevelComponentsStream() {
    return ast.topLevelComponentsStream();
  }

  @Override
  public Spliterator<ComponentAst> topLevelComponentsSpliterator() {
    return ast.topLevelComponentsSpliterator();
  }

  @Override
  public void updatePropertiesResolver(UnaryOperator<String> newPropertiesResolver) {
    ast.updatePropertiesResolver(newPropertiesResolver);
  }

}

