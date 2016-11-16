/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.stream.Collectors.toList;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.util.List;

/**
 * Verifies that if a source was developed using JAVA and it contains methods
 * annotated with {@link OnSuccess} and {@link OnError} which declare the same parameter,
 * such parameter is not reflected as a {@link ParameterModel} twice.
 * <p>
 * For example:
 * <p>
 * <pre>
 *   @OnSuccess
 *   public void onSuccess(String foo, String bar) {}
 *
 *   @OnError
 *   public void onError(String foo) {}
 * </pre>
 * <p>
 * This enricher makes sure that the source only has two parameters, foo and bar.
 *
 * @since 4.0
 */
public final class SourceCallbackSanitizerModelEnricher implements ModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration declaration) {
        declaration.getParameterGroups().forEach(group -> {
          List<ParameterDeclaration> callbackParameters = getCallbackParameters(group);
          if (!callbackParameters.isEmpty()) {
            group.getParameters().removeAll(callbackParameters);
            group.getParameters().addAll(sanitizeCallbackParameters(callbackParameters));
          }
        });
      }
    }.walk(describingContext.getExtensionDeclarer().getDeclaration());
  }

  private List<ParameterDeclaration> getCallbackParameters(ParameterGroupDeclaration declaration) {
    return declaration.getParameters().stream()
        .filter(p -> p.getModelProperty(CallbackParameterModelProperty.class).isPresent())
        .collect(toList());
  }

  private List<ParameterDeclaration> sanitizeCallbackParameters(List<ParameterDeclaration> callbackParameters) {
    return callbackParameters.stream()
        .map(ParameterWrapper::new)
        .distinct()
        .map(ParameterWrapper::getParameter)
        .collect(toList());
  }

  private class ParameterWrapper {

    private final ParameterDeclaration parameter;

    private ParameterWrapper(ParameterDeclaration parameter) {
      this.parameter = parameter;
    }

    public ParameterDeclaration getParameter() {
      return parameter;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ParameterWrapper) {
        ParameterWrapper other = (ParameterWrapper) obj;
        return parameter.getName().equals(other.parameter.getName()) && getType(parameter.getType())
            .equals(getType(other.parameter.getType()));
      }

      return false;
    }

    @Override
    public int hashCode() {
      return parameter.getName().hashCode();
    }
  }
}
