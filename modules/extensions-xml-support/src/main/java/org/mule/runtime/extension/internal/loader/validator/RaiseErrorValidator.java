/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validator;

import static java.lang.String.format;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.RAISE_ERROR_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.CORE_ERROR_NS;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.config.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.api.dsl.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.OperationComponentModelModelProperty;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which are XML based, as those that contain usages
 * of {@link ApplicationModel#RAISE_ERROR_IDENTIFIER} within an {@link OperationModel}.
 *
 * @since 4.0
 */
public class RaiseErrorValidator implements ExtensionModelValidator {

  public static final String TYPE_RAISE_ERROR_ATTRIBUTE = "type";
  private static final String TYPE_RAISE_ERROR_SEPARATOR = ":";
  public static final String RAISE_ERROR_EMPTY_TYPE_FORMAT_MESSAGE =
      "When using a %s the '%s' must not be null nor empty, offending operation '%s'";
  public static final String RAISE_ERROR_WRONG_TYPE_VALUE_FORMAT_MESSAGE =
      "When using a %s the '%s' must either use the runtime or the custom namespace of the current module ('%s' or '%s') but found '%s', offending operation '%s'";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel operationModel) {
        operationModel.getModelProperty(OperationComponentModelModelProperty.class)
            .ifPresent(operationComponentModelModelProperty -> {
              searchAndValidateRaiseError(extensionModel.getXmlDslModel().getPrefix(), operationModel,
                                          operationComponentModelModelProperty.getBodyComponentModel(), problemsReporter);
            });
      }
    }.walk(extensionModel);
  }

  /**
   * Goes over the complete set of message processors inside the <body/> declaration, checking if any of those is a {@link ApplicationModel#RAISE_ERROR_IDENTIFIER}.
   * If it is, then asserts the correct namespace of it (as XML <module/>s can throw exceptions of the the same namespace).
   *
   * @param namespace namespace of the <module/>
   * @param operationModel current operation of the <module/>
   * @param componentModel XML element to validate, or its child elements.
   * @param problemsReporter gatherer of errors
   */
  private void searchAndValidateRaiseError(String namespace, OperationModel operationModel, ComponentModel componentModel,
                                           ProblemsReporter problemsReporter) {
    if (componentModel.getIdentifier().equals(RAISE_ERROR_IDENTIFIER)) {
      validateRaiseError(namespace, operationModel, componentModel, problemsReporter);
    }
    for (ComponentModel childComponentModel : componentModel.getInnerComponents()) {
      searchAndValidateRaiseError(namespace, operationModel, childComponentModel, problemsReporter);
    }
  }

  private void validateRaiseError(String moduleNamespace, OperationModel operationModel, ComponentModel raiseErrorComponentModel,
                                  ProblemsReporter problemsReporter) {

    final String stringRepresentation = raiseErrorComponentModel.getParameters().get(TYPE_RAISE_ERROR_ATTRIBUTE);
    if (StringUtils.isBlank(stringRepresentation)) {
      problemsReporter.addError(new Problem(operationModel, format(
                                                                   RAISE_ERROR_EMPTY_TYPE_FORMAT_MESSAGE,
                                                                   RAISE_ERROR_IDENTIFIER.toString(),
                                                                   TYPE_RAISE_ERROR_ATTRIBUTE,
                                                                   operationModel.getName())));
    } else {
      int separator = stringRepresentation.indexOf(TYPE_RAISE_ERROR_SEPARATOR);
      if (separator > 0) {
        String namespace = stringRepresentation.substring(0, separator).toUpperCase();
        if (!moduleNamespace.toUpperCase().equals(namespace)) {
          problemsReporter.addError(new Problem(operationModel, format(
                                                                       RAISE_ERROR_WRONG_TYPE_VALUE_FORMAT_MESSAGE,
                                                                       RAISE_ERROR_IDENTIFIER.toString(),
                                                                       TYPE_RAISE_ERROR_ATTRIBUTE,
                                                                       CORE_ERROR_NS,
                                                                       moduleNamespace.toUpperCase(),
                                                                       namespace,
                                                                       operationModel.getName())));
        }
      }
    }
  }
}
