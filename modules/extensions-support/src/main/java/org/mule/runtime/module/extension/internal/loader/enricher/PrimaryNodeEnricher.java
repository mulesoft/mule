/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.PRIMARY_NODE_ONLY_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.PRIMARY_NODE_ONLY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.annotation.source.PrimaryNodeOnly;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * Adds a #{@link ExtensionConstants#PRIMARY_NODE_ONLY_PARAMETER_NAME} parameter on all sources for which
 * {@link SourceModel#runsOnPrimaryNodeOnly()} is {@code false}
 *
 * @since 1.1
 */
public class PrimaryNodeEnricher extends AbstractAnnotatedDeclarationEnricher {

  private MetadataType booleanType = create(JAVA).booleanType().build();

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration declaration) {
        if (isPrimaryNodeOnly(declaration)) {
          declaration.setRunsOnPrimaryNodeOnly(true);
        } else {
          declaration.setRunsOnPrimaryNodeOnly(false);

          ParameterDeclaration parameter = new ParameterDeclaration(PRIMARY_NODE_ONLY_PARAMETER_NAME);
          parameter.setDescription(PRIMARY_NODE_ONLY_PARAMETER_DESCRIPTION);
          parameter.setType(booleanType, false);
          parameter.setExpressionSupport(NOT_SUPPORTED);
          parameter.setRequired(false);
          parameter.setDefaultValue(false);
          parameter.setLayoutModel(LayoutModel.builder().tabName(ADVANCED_TAB).build());

          declaration.getParameterGroup(DEFAULT_GROUP_NAME).addParameter(parameter);
        }
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private boolean isPrimaryNodeOnly(SourceDeclaration declaration) {
    return extractAnnotation(declaration, PrimaryNodeOnly.class) != null;
  }
}
