/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.properties;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.internal.dsl.model.DefaultConfigurationParameters;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class ConfigurationPropertiesProviderFactoryUtils {

  public static ConfigurationParameters resolveConfigurationParameters(DefaultConfigurationParameters.Builder configurationParametersBuilder,
                                                                       ComponentAst component,
                                                                       UnaryOperator<String> localResolver) {
    component.getModel(ParameterizedModel.class)
        .ifPresent(pmzd -> pmzd.getParameterGroupModels()
            .forEach(pmg -> {
              if (pmg.isShowInDsl()) {
                DslElementSyntax childSyntax =
                    component.getGenerationInformation().getSyntax().get().getChild(pmg.getName()).get();
                ComponentIdentifier dslGroupIdentifier = ComponentIdentifier.builder().name(childSyntax.getElementName())
                    .namespace(childSyntax.getPrefix())
                    .namespaceUri(childSyntax.getNamespace())
                    .build();

                DefaultConfigurationParameters.Builder dslGroupParametersBuilder = DefaultConfigurationParameters.builder();
                resolveParamGroupConfigurationParameters(dslGroupParametersBuilder, component, pmg, localResolver);
                configurationParametersBuilder.withComplexParameter(dslGroupIdentifier,
                                                                    dslGroupParametersBuilder.build());

              } else {
                resolveParamGroupConfigurationParameters(configurationParametersBuilder, component, pmg, localResolver);
              }
            }));

    component
        .directChildrenStream()
        .forEach(child -> configurationParametersBuilder
            .withComplexParameter(child.getIdentifier(),
                                  resolveConfigurationParameters(DefaultConfigurationParameters.builder(),
                                                                 child,
                                                                 localResolver)));

    return configurationParametersBuilder.build();
  }

  private static void resolveParamGroupConfigurationParameters(DefaultConfigurationParameters.Builder configurationParametersBuilder,
                                                               ComponentAst component, ParameterGroupModel parameterGroup,
                                                               UnaryOperator<String> localResolver) {
    parameterGroup.getParameterModels()
        .forEach(pm -> {
          ComponentParameterAst param = component.getParameter(parameterGroup.getName(), pm.getName());
          if (param != null) {
            param.getModel().getType().accept(new MetadataTypeVisitor() {

              @Override
              public void visitArrayType(ArrayType arrayType) {
                getParamComponentIdentifier(param).ifPresent(paramComponentIdentifier -> {
                  DefaultConfigurationParameters.Builder childParametersBuilder = DefaultConfigurationParameters.builder();

                  visitMultiple(localResolver, param, childParametersBuilder);

                  configurationParametersBuilder.withComplexParameter(paramComponentIdentifier,
                                                                      childParametersBuilder.build());
                });
              }

              @Override
              public void visitObject(ObjectType objectType) {
                getParamComponentIdentifier(param).ifPresent(paramComponentIdentifier -> {
                  DefaultConfigurationParameters.Builder childParametersBuilder = DefaultConfigurationParameters.builder();

                  if (isMap(objectType)) {
                    visitMultiple(localResolver, param, childParametersBuilder);
                  } else {
                    Object value = param.getValue().getRight();
                    if (value instanceof ComponentAst) {
                      resolveConfigurationParameters(childParametersBuilder, (ComponentAst) value, localResolver);
                    }
                  }

                  configurationParametersBuilder.withComplexParameter(paramComponentIdentifier,
                                                                      childParametersBuilder.build());
                });
              }

              protected Optional<ComponentIdentifier> getParamComponentIdentifier(ComponentParameterAst param) {
                return param.getGenerationInformation().getSyntax()
                    .map(paramSyntax -> ComponentIdentifier.builder()
                        .namespaceUri(paramSyntax.getNamespace())
                        .namespace(paramSyntax.getPrefix())
                        .name(paramSyntax.getElementName())
                        .build());
              }

              protected void visitMultiple(UnaryOperator<String> localResolver, ComponentParameterAst param,
                                           DefaultConfigurationParameters.Builder childParametersBuilder) {
                Object value = param.getValue().getRight();
                if (value != null && value instanceof Collection) {
                  ((Collection) value).forEach(item -> {
                    DefaultConfigurationParameters.Builder itemBuilder = DefaultConfigurationParameters.builder();
                    if (item instanceof ComponentAst) {
                      resolveConfigurationParameters(itemBuilder, (ComponentAst) item, localResolver);
                      childParametersBuilder.withComplexParameter(((ComponentAst) item).getIdentifier(),
                                                                  itemBuilder.build());
                    }
                  });
                }
              }

              @Override
              public void defaultVisit(MetadataType metadataType) {
                configurationParametersBuilder
                    .withSimpleParameter(pm.getName(), localResolver.apply(param.getResolvedRawValue()));
              }
            });
          }
        });
  }

}
