/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsOfType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Validates that no {@link Source} implementation has more than one {@link Field} of type {@link ComponentLocation}
 *
 * @since 4.0
 */
public class ComponentLocationModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onSource(SourceModel model) {
        model.getModelProperty(ImplementingTypeModelProperty.class)
            .map(ImplementingTypeModelProperty::getType)
            .ifPresent(sourceType -> {

              List<Field> fields = getFieldsOfType(sourceType, ComponentLocation.class);

              if (fields.size() > 1) {
                problemsReporter.addError(new Problem(model, format(
                                                                    "Source of type '%s' has %d fields of type '%s'. Only one is allowed",
                                                                    sourceType.getName(), fields.size(),
                                                                    ComponentLocation.class.getSimpleName())));
              }
            });
      }
    }.walk(model);
  }
}
