/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceResult;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * {@link ExtensionModelValidator} which verifies that the On Terminate callback for Sources is not configured
 * with not allowed parameters.
 *
 * @since 1.0
 */
public class SourceCallbacksModelValidator implements ExtensionModelValidator {

  private static final String SOURCE_RESULT = SourceResult.class.getSimpleName();
  private static final String CALLBACK_CONTEXT = SourceCallbackContext.class.getSimpleName();
  private static final String ERROR_MESSAGE =
      String.format("@%s callback method can only receive parameters of the following types: " +
          "'%s' and '%s'", OnTerminate.class.getSimpleName(), SOURCE_RESULT, CALLBACK_CONTEXT);

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onSource(SourceModel sourceModel) {
        Optional<SourceCallbackModel> successCallback = sourceModel.getSuccessCallback();
        Optional<SourceCallbackModel> errorCallback = sourceModel.getErrorCallback();
        Optional<SourceCallbackModel> terminateCallback = sourceModel.getTerminateCallback();

        if (hasCallbacksWithOutOnTerminate(successCallback, errorCallback, terminateCallback)) {
          if (!errorCallback.isPresent() || hasParameters(errorCallback)) {
            problemsReporter.addError(new Problem(sourceModel, getMissingTerminateCallbackError(sourceModel)));
          }
        }

        terminateCallback.ifPresent(callback -> {
          List<ParameterModel> parameters = callback.getAllParameterModels();
          if (!parameters.isEmpty()) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            parameters.forEach(param -> joiner.add(param.getName()));
            problemsReporter
                .addError(new Problem(sourceModel, format(ERROR_MESSAGE + ". Offending parameters: %s", joiner.toString())));
          }
        });
        this.stop();
      }
    }.walk(model);
  }

  private boolean hasCallbacksWithOutOnTerminate(Optional<SourceCallbackModel> successCallback,
                                                 Optional<SourceCallbackModel> errorCallback,
                                                 Optional<SourceCallbackModel> terminateCallback) {
    return (successCallback.isPresent() || errorCallback.isPresent()) && !terminateCallback.isPresent();
  }

  private boolean hasParameters(Optional<SourceCallbackModel> errorCallback) {
    return errorCallback.isPresent() && !errorCallback.get().getAllParameterModels().isEmpty();
  }

  private String getMissingTerminateCallbackError(SourceModel sourceModel) {
    return String.format("The source %s defines methods annotated with either @%s and/or @%s. " +
        "If at least one of these are defined, another method annotated with @%s is required.",
                         sourceModel.getName(), OnSuccess.class.getSimpleName(), OnError.class.getSimpleName(),
                         OnTerminate.class.getSimpleName());
  }
}
