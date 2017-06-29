/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ADVANCED_TAB_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingMethod;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

/**
 * Enriches operations which were defined in methods annotated with {@link DataTypeParameters} so that parameters
 * {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} and {@link ExtensionProperties#ENCODING_PARAMETER_NAME}. are added Both
 * attributes are optional, have no default value and accept expressions.
 *
 * @since 4.0
 */
public final class DataTypeDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  private final MetadataType STRING_TYPE = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader().load(String.class);

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    final ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    new IdempotentDeclarationWalker() {

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        getImplementingMethod(declaration).ifPresent(method -> {
          DataTypeParameters annotation = method.getAnnotation(DataTypeParameters.class);
          if (annotation != null) {
            if (isVoid(method)) {
              throw new IllegalModelDefinitionException(String.format(
                                                                      "Operation '%s' of extension '%s' is void yet requires the ability to change the content metadata."
                                                                          + " Mutating the content metadata requires an operation with a return type.",
                                                                      declaration.getName(), declaration.getName()));
            }

            declaration.getParameterGroup(DEFAULT_GROUP_NAME).addParameter(
                                                                           newParameter(MIME_TYPE_PARAMETER_NAME,
                                                                                        "The mime type of the payload that this operation outputs."));
            declaration.getParameterGroup(DEFAULT_GROUP_NAME)
                .addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding of the payload that this operation outputs."));
          }
        });
      }
    }.walk(declaration);
  }

  private ParameterDeclaration newParameter(String name, String description) {
    ParameterDeclaration parameter = new ParameterDeclaration(name);
    parameter.setRequired(false);
    parameter.setExpressionSupport(SUPPORTED);
    parameter.setType(STRING_TYPE, false);
    parameter.setDescription(description);
    parameter.setLayoutModel(LayoutModel.builder().tabName(ADVANCED_TAB_NAME).build());

    return parameter;
  }
}
