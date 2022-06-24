/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
