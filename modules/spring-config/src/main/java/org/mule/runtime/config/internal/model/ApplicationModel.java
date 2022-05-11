/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor.AST_POST_PROCESSORS;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.CRON_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EXPIRATION_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An {@code ApplicationModel} holds a representation of all the artifact configuration using an abstract model to represent any
 * configuration option.
 * <p/>
 * This model is represented by a set of {@link ComponentAst}a. Each {@code ComponentAst} holds a piece of configuration and may
 * have children {@code ComponentAst}s as defined in the artifact configuration.
 * <p/>
 * Once the set of {@code ComponentAst}s gets created from the application config, the {@code ApplicationModel} executes a set of
 * common validations dictated by the configuration semantics.
 *
 * @since 4.0
 */
public abstract class ApplicationModel {

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

  public static final String REDELIVERY_POLICY_ELEMENT = REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
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

  public static final ComponentIdentifier SCHEDULING_STRATEGY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier FIXED_FREQUENCY_STRATEGY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier CRON_STRATEGY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CRON_STRATEGY_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier TLS_CONTEXT_IDENTIFIER =
      builder().namespace(TLS_PREFIX).name(TLS_CONTEXT_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier TLS_REVOCATION_CHECK_IDENTIFIER =
      builder().namespace(TLS_PREFIX).name(TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier POOLING_PROFILE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(POOLING_PROFILE_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier EXPIRATION_POLICY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(EXPIRATION_POLICY_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier RECONNECT_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RECONNECT_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier RECONNECT_FOREVER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RECONNECT_FOREVER_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier RECONNECTION_CONFIG_PARAMETER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RECONNECTION_CONFIG_PARAMETER_NAME).build();

  public static final ComponentIdentifier REPEATABLE_IN_MEMORY_STREAM_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS).build();
  public static final ComponentIdentifier REPEATABLE_FILE_STORE_STREAM_IDENTIFIER =
      builder().namespace(EE_PREFIX).name(REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS).build();
  public static final ComponentIdentifier NON_REPEATABLE_STREAM_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(NON_REPEATABLE_BYTE_STREAM_ALIAS).build();

  public static final ComponentIdentifier REPEATABLE_IN_MEMORY_ITERABLE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS).build();
  public static final ComponentIdentifier REPEATABLE_FILE_STORE_ITERABLE_IDENTIFIER =
      builder().namespace(EE_PREFIX).name(REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS).build();
  public static final ComponentIdentifier NON_REPEATABLE_ITERABLE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(NON_REPEATABLE_OBJECTS_STREAM_ALIAS).build();

  /**
   * Preprocesses the ArtifactAst so that it can be deployed to runtime.
   *
   * @param extensionModels Extension models to be used when macro expand the current {@link ApplicationModel}
   */
  public static ArtifactAst prepareAstForRuntime(ArtifactAst ast, Set<ExtensionModel> extensionModels) {
    ast = processSourcesRedeliveryPolicy(ast);

    for (ApplicationModelAstPostProcessor astPostProcessor : AST_POST_PROCESSORS.get()) {
      ast = astPostProcessor.postProcessAst(ast, extensionModels);
    }

    return ast;
  }

  /**
   * Process from any message source the redelivery-policy to make it part of the final pipeline.
   */
  private static ArtifactAst processSourcesRedeliveryPolicy(ArtifactAst ast) {
    return copyRecursively(ast, flow -> {

      if (!FLOW_IDENTIFIER.equals(flow.getIdentifier())) {
        return flow;
      }

      return flow.directChildrenStream().findFirst()
          .filter(comp -> comp.getModel(SourceModel.class).isPresent())
          .flatMap(comp -> getGroupAndParametersPairs(comp.getModel(SourceModel.class).get())
              .filter(pairGroupSource -> {
                final ComponentParameterAst redeliveryPolicyParam =
                    comp.getParameter(pairGroupSource.getFirst().getName(), REDELIVERY_POLICY_PARAMETER_NAME);
                if (redeliveryPolicyParam != null) {
                  final ComponentAst redeliveryPolicy = (ComponentAst) redeliveryPolicyParam.getValue().getRight();
                  return redeliveryPolicy != null;
                }
                return false;
              })
              .map(pairGroupSource -> {
                final ComponentAst redeliveryPolicy = (ComponentAst) comp
                    .getParameter(pairGroupSource.getFirst().getName(), REDELIVERY_POLICY_PARAMETER_NAME).getValue().getRight();
                return transformFlowWithRedeliveryPolicy(flow, comp, redeliveryPolicy);
              })
              .findFirst())
          .orElse(flow);
    });
  }

  private static ComponentAst transformFlowWithRedeliveryPolicy(ComponentAst flow, ComponentAst source,
                                                                ComponentAst redeliveryPolicy) {
    final List<ComponentAst> newFlowChildren = new ArrayList<>();

    // The transformed source is the same with the redelivery-policy removed...
    newFlowChildren.add(new BaseComponentAstDecorator(source) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        return super.directChildrenStream()
            .filter(sourceChild -> sourceChild != redeliveryPolicy);
      }

      @Override
      public ComponentParameterAst getParameter(String groupName, String paramName) {
        if (REDELIVERY_POLICY_PARAMETER_NAME.equals(paramName)) {
          return null;
        } else {
          return getDecorated().getParameter(groupName, paramName);
        }
      };

      @Override
      public Collection<ComponentParameterAst> getParameters() {
        return getDecorated().getParameters().stream()
            .filter(p -> !p.getModel().getName().equals(REDELIVERY_POLICY_PARAMETER_NAME))
            .collect(toList());
      };

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

  public static Optional<ComponentAst> findComponentDefinitionModel(ArtifactAst ast, ComponentIdentifier componentIdentifier) {
    return ast.topLevelComponentsStream()
        .filter(componentModel -> componentModel.getIdentifier().equals(componentIdentifier)).findFirst();
  }
}
