/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import org.mule.runtime.extension.api.runtime.source.PollingSource;

import javax.xml.namespace.QName;

public class PollingSourceDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration source) {
        extractImplementingType(source).ifPresent(type -> {
          if (PollingSource.class.isAssignableFrom(type)) {
            ParameterDeclaration parameter = new ParameterDeclaration(SCHEDULING_STRATEGY_PARAMETER_NAME);
            parameter.setDescription(SCHEDULING_STRATEGY_PARAMETER_DESCRIPTION);
            parameter.setRequired(true);
            parameter.setType(loader.load(Scheduler.class), false);
            parameter.setExpressionSupport(NOT_SUPPORTED);
            parameter.addModelProperty(new InfrastructureParameterModelProperty(10));
            parameter.addModelProperty(new QNameModelProperty(new QName(CORE_NAMESPACE, SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER,
                                                                        CORE_PREFIX)));
            parameter.setDslConfiguration(ParameterDslConfiguration.builder()
                .allowsInlineDefinition(true)
                .allowsReferences(false)
                .allowTopLevelDefinition(false)
                .build());


            source.getParameterGroup(DEFAULT_GROUP_NAME).addParameter(parameter);
          }
        });
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }
}
