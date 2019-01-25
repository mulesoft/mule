/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isASTMode;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * Validates that POJOs parameter overrides Equals and HashCode
 *
 * @since 4.2
 */
public class EqualsAndHashCodeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    if (!isCompiletime(extensionModel) || isASTMode(extensionModel)) {
      return;
    }

    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        validateOverridesEqualsAndHashCode(model, problemsReporter);
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        validateOverridesEqualsAndHashCode(model, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateOverridesEqualsAndHashCode(ParameterizedModel model, ProblemsReporter reporter) {
    model.getAllParameterModels().forEach(parameterModel -> parameterModel.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Class<?> clazz = getType(objectType).orElse(null);

        if ((clazz != null) && !clazz.isInterface() &&
            !isAbstract(clazz.getModifiers())
            && (!overridesEqualsAndHashCode(clazz))) {
          reporter
              .addError(new Problem(model,
                                    format("Type '%s' must override equals and hashCode",
                                           clazz.getSimpleName())));
        }
      }
    }));

  }

  private boolean overridesEqualsAndHashCode(Class<?> clazz) {
    try {
      return (!clazz.getMethod("equals", Object.class).getDeclaringClass().equals(Object.class))
          && (!clazz.getMethod("hashCode").getDeclaringClass().equals(Object.class));
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
