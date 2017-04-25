/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.List;

/**
 * Utility class for {@link ExtensionModelValidator}s
 *
 * @since 4.0
 */
final class ModelValidationUtils {

  private ModelValidationUtils() {}

  static void validateConfigOverrideParametersNotAllowed(ParameterizedModel model, ProblemsReporter reporter, String kind) {

    List<String> configOverrideParameters = model.getAllParameterModels().stream()
        .filter(ParameterModel::isOverrideFromConfig)
        .map(ParameterModel::getName)
        .collect(toList());

    if (!configOverrideParameters.isEmpty()) {
      reporter.addError(new Problem(model, format(
                                                  "%s '%s' declares the parameters %s as '%s', which is not allowed for this component parameters",
                                                  kind,
                                                  model.getName(),
                                                  configOverrideParameters.toString(),
                                                  ConfigOverride.class.getSimpleName(), kind)));
    }
  }

}
