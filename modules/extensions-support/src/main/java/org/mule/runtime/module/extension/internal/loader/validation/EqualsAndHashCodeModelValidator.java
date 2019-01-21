/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;

/**
 * Validates that POJOs parameter overrides Equals and HashCode
 *
 * @since 4.0
 */
public class EqualsAndHashCodeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    if (!isCompiletime(extensionModel)) {
      return;
    }

    new ExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        model.getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            model.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
                .map(descriptor -> descriptor.getExtensionParameter().getType())
                .ifPresent(type -> {

                  Class<?> clazz = type.getDeclaringClass().get();

                  if (!clazz.isInterface() &&
                      !isAbstract(clazz.getModifiers())
                      && (!overridesEqualsAndHashCode(clazz))) {
                    problemsReporter
                        .addError(new Problem(model, format("Type '%s' must override equals and hashCode", type.getName())));
                  }

                });
          }
        });
      }
    }.walk(extensionModel);
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
