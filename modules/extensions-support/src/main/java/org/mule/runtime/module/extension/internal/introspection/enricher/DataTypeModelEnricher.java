/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingMethod;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.Method;

/**
 * Enriches operations which were defined in methods annotated with {@link DataTypeParameters} so that parameters
 * {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} and {@link ExtensionProperties#ENCODING_PARAMETER_NAME}. are added Both
 * attributes are optional, have no default value and accept expressions.
 *
 * @since 4.0
 */
public final class DataTypeModelEnricher extends AbstractAnnotatedModelEnricher {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public void enrich(DescribingContext describingContext) {
    final ExtensionDeclaration declaration = describingContext.getExtensionDeclarer().getDeclaration();
    new IdempotentDeclarationWalker() {

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        Method method = getImplementingMethod(declaration);
        if (method != null) {
          DataTypeParameters annotation = method.getAnnotation(DataTypeParameters.class);
          if (annotation != null) {
            if (isVoid(method)) {
              throw new IllegalModelDefinitionException(String.format(
                                                                      "Operation '%s' of extension '%s' is void yet requires the ability to change the content metadata."
                                                                          + " Mutating the content metadata requires an operation with a return type.",
                                                                      declaration.getName(), declaration.getName()));
            }
            declaration.addParameter(newParameter(MIME_TYPE_PARAMETER_NAME,
                                                  "The mime type of the payload that this operation outputs."));
            declaration
                .addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding of the payload that this operation outputs."));
          }
        }
      }
    }.walk(declaration);
  }

  private ParameterDeclaration newParameter(String name, String description) {
    ParameterDeclaration parameter = new ParameterDeclaration(name);
    parameter.setRequired(false);
    parameter.setExpressionSupport(SUPPORTED);
    parameter.setType(typeLoader.load(String.class), false);
    parameter.setDescription(description);

    return parameter;
  }
}
