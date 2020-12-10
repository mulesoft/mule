/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.internal.model.properties.PropertiesResolverUtils.createConfigurationAttributeResolver;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.slf4j.Logger;

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

  private ArtifactAst ast;
  private final ArtifactAst originalAst;
  private final PropertiesResolverConfigurationProperties configurationProperties;
  private final Map<String, ComponentAst> namedTopLevelComponentModels = new HashMap<>();

  /**
   * Creates an {code ApplicationModel} from an {@link ArtifactAst}.
   * <p/>
   * A set of validations are applied that may make creation fail.
   *
   * @param artifactAst the mule artifact configuration content.
   * @param deploymentProperties values for replacement of properties in the DSL
   * @param parentConfigurationProperties the {@link ConfigurationProperties} of the parent artifact. For instance, application
   *        will receive the domain resolver.
   * @param externalResourceProvider the provider for configuration properties files and ${file::name.txt} placeholders
   */
  public ApplicationModel(ArtifactAst artifactAst,
                          Map<String, String> deploymentProperties,
                          Optional<ConfigurationProperties> parentConfigurationProperties,
                          ResourceProvider externalResourceProvider,
                          FeatureFlaggingService featureFlaggingService) {
    this.originalAst = artifactAst;

    this.configurationProperties = createConfigurationAttributeResolver(originalAst, parentConfigurationProperties,
                                                                        deploymentProperties, externalResourceProvider,
                                                                        featureFlaggingService);
    try {
      initialiseIfNeeded(configurationProperties.getConfigurationPropertiesResolver());
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }

    this.originalAst
        .updatePropertiesResolver(configurationProperties.getConfigurationPropertiesResolver());

    this.ast = originalAst;
    indexComponentModels(originalAst);
  }

  private void indexComponentModels(ArtifactAst originalAst) {
    originalAst.topLevelComponentsStream()
        .forEach(componentModel -> componentModel.getComponentId()
            .ifPresent(name -> namedTopLevelComponentModels.put(name, componentModel)));
  }

  /**
   * Preprocesses the ArtifactAst so that it can be deployed to runtime.
   *
   * @param extensionModels Extension models to be used when macro expand the current {@link ApplicationModel}
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
   * @param ast
   * @param extensionModels Set of {@link ExtensionModel extensionModels} that will be used to check if the element has to be
   *        expanded.
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

  public Optional<ComponentAst> findComponentDefinitionModel(ComponentIdentifier componentIdentifier) {
    return topLevelComponentsStream()
        .filter(componentModel -> componentModel.getIdentifier().equals(componentIdentifier)).findFirst();
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
