/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecableModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * Validates that the parameters which are annotated with {@link org.mule.runtime.extension.api.annotation.deprecated.Deprecated}
 * are not required.
 *
 * @since 4.2.0
 */
public class DeprecationModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {

    if (!isCompiletime(extensionModel)) {
      return;
    }

    validateDeprecationVersions(extensionModel, problemsReporter);

    new IdempotentExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateDeprecationVersions(model, problemsReporter);
        if (model.isRequired() && model.isDeprecated()) {
          problemsReporter.addError(new Problem(model,
                                                format("Parameter \"%s\" from %s named \"%s\" is required but has been marked as deprecated. Required parameters cannot be deprecated. Use the @Optional annotation to make it optional and add a default value.",
                                                       model.getName(), getComponentModelTypeName(owner), owner.getName())));
        }
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

      @Override
      protected void onSource(SourceModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

      @Override
      protected void onConstruct(ConstructModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

      @Override
      protected void onOperation(OperationModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

      @Override
      protected void onFunction(FunctionModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        validateDeprecationVersions(model, problemsReporter);
      }

    }.walk(extensionModel);

  }

  private void validateDeprecationVersions(DeprecableModel deprecableModel, ProblemsReporter problemsReporter) {
    deprecableModel.getDeprecationModel().ifPresent(deprecationModel -> {
      MuleVersion deprecatedSince = null;
      MuleVersion toRemoveIn = null;
      try {
        deprecatedSince = new MuleVersion(deprecationModel.getDeprecatedSince());
      } catch (IllegalArgumentException e) {
        reportInvalidVersion(deprecationModel.getDeprecatedSince(), deprecableModel, problemsReporter, "since");
      }
      try {
        toRemoveIn = deprecationModel.getToRemoveIn().map(toRemoveInString -> new MuleVersion(toRemoveInString)).orElse(null);
      } catch (IllegalArgumentException e) {
        deprecationModel.getToRemoveIn()
            .ifPresent(toRemoveInString -> reportInvalidVersion(toRemoveInString, deprecableModel, problemsReporter,
                                                                "toRemoveIn"));
      }

      if (deprecatedSince != null && toRemoveIn != null && !deprecatedSince.priorTo(toRemoveIn)) {
        reportSinceVersionPriorToRemoveVersion(deprecatedSince, toRemoveIn, deprecableModel, problemsReporter);
      }

    });
  }

  private void reportInvalidVersion(String versionString, DeprecableModel deprecableModel, ProblemsReporter problemsReporter,
                                    String versionType) {
    if (deprecableModel instanceof NamedObject) {
      problemsReporter.addError(new Problem((NamedObject) deprecableModel,
                                            format("The %s named %s was deprecated with an invalid '%s' version : '%s' . This version must follow the semver convention",
                                                   getModelTypeName(deprecableModel),
                                                   ((NamedObject) deprecableModel).getName(), versionType,
                                                   versionString)));
    }
  }

  private void reportSinceVersionPriorToRemoveVersion(MuleVersion deprecatedSince, MuleVersion toRemoveIn,
                                                      DeprecableModel deprecableModel, ProblemsReporter problemsReporter) {
    if (deprecableModel instanceof NamedObject) {
      problemsReporter
          .addError(new Problem((NamedObject) deprecableModel,
                                format("The versions chosen for the deprecation of the %s named %s are invalid, `since`(%s) version must be prior to the `removeTo`(%s) version.",
                                       getModelTypeName(deprecableModel), ((NamedObject) deprecableModel).getName(),
                                       deprecatedSince.toString(), toRemoveIn.toString())));
    }
  }

  private String getModelTypeName(DeprecableModel deprecableModel) {
    return deprecableModel instanceof ParameterizedModel ? getComponentModelTypeName((ParameterizedModel) deprecableModel)
        : deprecableModel instanceof ExtensionModel ? "extension" : "parameter";
  }

}
