/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.property.SyntheticModelModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;

import java.util.List;

/**
 * Utility class for {@link ExtensionModelValidator}s
 *
 * @since 4.0
 */
public final class JavaModelValidationUtils {

  private JavaModelValidationUtils() {}

  public static boolean isCompiletime(ExtensionModel model) {
    return model.getModelProperty(CompileTimeModelProperty.class).isPresent();
  }

}
