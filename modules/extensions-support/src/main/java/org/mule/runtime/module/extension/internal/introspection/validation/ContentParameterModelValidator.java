/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;

import com.google.common.base.Joiner;

import java.util.List;

/**
 * Validates that all content parameters are property configured.
 * <p>
 * For configs and connection providers, tt validates that they do not have content parameters.
 * For Operations and sources, it validates that content parameters meet the following:
 * <p>
 * <ul>
 * <li>Support or require expressions</li>
 * <li>They don't allow DSL references</li>
 * <li>There's no more than one {@link ParameterRole#PRIMARY_CONTENT}</li>
 * <li>{@link ParameterRole#PRIMARY_CONTENT} parameter is optional and defaults to {@link Optional#PAYLOAD}</li>
 * </ul>
 *
 * @since 4.0
 */
public class ContentParameterModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    new IdempotentExtensionWalker() {

      @Override
      public void onConfiguration(ConfigurationModel model) {
        validateNoContent(extensionModel, model);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        validateNoContent(extensionModel, model);
      }

      @Override
      protected void onOperation(OperationModel model) {
        validateContent(extensionModel, model);
      }

      @Override
      protected void onSource(SourceModel model) {
        validateContent(extensionModel, model);
      }
    }.walk(extensionModel);
  }

  private void validateNoContent(ExtensionModel extensionModel, ParameterizedModel model) {
    List<ParameterModel> contentParameters = getContentParameters(model);

    if (!contentParameters.isEmpty()) {
      throw modelException(extensionModel, model,
                           format("which contains content parameters. Content parameters are not allowed on %s components",
                                  getComponentModelTypeName(model)));
    }
  }

  private void validateContent(ExtensionModel extensionModel, ParameterizedModel model) {
    List<ParameterModel> contentParameters = getContentParameters(model);

    if (contentParameters.isEmpty()) {
      return;
    }

    validatePrimaryContent(extensionModel, model, contentParameters);
    validateDsl(extensionModel, model, contentParameters);
    validateExpressionSupport(extensionModel, model, contentParameters);
  }

  private void validateExpressionSupport(ExtensionModel extensionModel, ParameterizedModel model,
                                         List<ParameterModel> contentParameters) {
    List<ParameterModel> expressionLess = contentParameters.stream()
        .filter(p -> p.getExpressionSupport() == NOT_SUPPORTED)
        .collect(toList());

    if (!expressionLess.isEmpty()) {
      throw modelException(extensionModel, model,
                           format("which contains content parameters which don't allow expressions. Expressions"
                               + " are mandatory for all content parameters. Offending parameters are: [%s]",
                                  join(expressionLess)));
    }
  }

  private void validateDsl(ExtensionModel extensionModel, ParameterizedModel model, List<ParameterModel> contentParameters) {
    List<ParameterModel> offending = contentParameters.stream()
        .filter(p -> p.getDslModel().allowsReferences())
        .collect(toList());

    if (!offending.isEmpty()) {
      throw modelException(extensionModel, model, format("which contains content parameters which allow references. "
          + "Offending parameters are: [%s]", join(offending)));
    }
  }

  private IllegalModelDefinitionException modelException(ExtensionModel extensionModel, ParameterizedModel model,
                                                         String message) {
    return new IllegalModelDefinitionException(format("Extension '%s' defines %s '%s' %s",
                                                      extensionModel.getName(),
                                                      getComponentModelTypeName(model),
                                                      model.getName(),
                                                      message));
  }

  private void validatePrimaryContent(ExtensionModel extensionModel, ParameterizedModel model,
                                      List<ParameterModel> contentParameters) {
    List<ParameterModel> primaryContents = contentParameters.stream()
        .filter(p -> p.getRole() == PRIMARY_CONTENT)
        .collect(toList());

    if (primaryContents.isEmpty()) {
      throw modelException(extensionModel, model,
                           format("which contains %d content parameters but none of them is primary", primaryContents.size()));
    } else if (primaryContents.size() > 1) {
      throw modelException(extensionModel, model,
                           format("which contains %d content parameters marked as primary. Only one primary "
                               + "content parameter is allowed. Offending parameters are [%s]",
                                  primaryContents.size(),
                                  join(primaryContents)));
    }

    validateDefaultsToPayload(extensionModel, model, primaryContents.get(0));
  }

  private void validateDefaultsToPayload(ExtensionModel extensionModel, ParameterizedModel model, ParameterModel parameter) {
    if (!PAYLOAD.equals(parameter.getDefaultValue())) {
      throw modelException(extensionModel, model, format("which contains parameter '%s' which is set as primary content "
          + "but does not default to the payload", parameter.getName()));
    }
  }

  private List<ParameterModel> getContentParameters(ParameterizedModel model) {
    return model.getAllParameterModels().stream().filter(ExtensionModelUtils::isContent).collect(toList());
  }

  private String join(List<ParameterModel> offending) {
    return Joiner.on(", ").join(offending);
  }
}
