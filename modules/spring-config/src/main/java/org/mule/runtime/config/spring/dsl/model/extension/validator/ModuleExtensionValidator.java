/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension.validator;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.ParameterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple validations for a module, such as: prohibiting names with spaces, invalid chars, etc.
 */
public class ModuleExtensionValidator {

  private final Set<String> PROPERTY_RESERVED_NAMES =
      Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"name"})));
  private final Set<String> PARAMETER_RESERVED_NAMES =
      Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"config-ref", "target"})));

  private final Pattern VALID_TAG_NAME = Pattern.compile("[A-Za-z]([A-Za-z0-9\\-\\_]*)");

  public void validate(ModuleExtension model) {
    ErrorGatherer gatherer = new ErrorGatherer();

    doValidate(model, gatherer);

    if (!gatherer.getErrors().isEmpty()) {
      String errorString = gatherer.getErrors().size() == 1 ? "error" : "errors";
      StringBuilder sb = new StringBuilder(format("There are [%d] %s for the module [%s]:", gatherer.getErrors().size(),
                                                  errorString, model.getName()))
                                                      .append(System.getProperty("line.separator"));

      sb.append(join(System.getProperty("line.separator"), gatherer.getErrors()));
      throw new MuleRuntimeException(createStaticMessage(sb.toString()));
    }
  }

  private void doValidate(ModuleExtension model, ErrorGatherer gatherer) {
    validateParameters(gatherer, model.getProperties(), "property", PROPERTY_RESERVED_NAMES);
    model.getOperations().forEach((operationName, operationExtension) -> {
      if (!VALID_TAG_NAME.matcher(operationExtension.getName()).matches()) {
        gatherer.addError(String.format("The operation [%s] has invalid characters for an XML element",
                                        operationExtension.getName()));
      }
      ErrorGatherer gathererForOperation = new ErrorGatherer();
      validateParameters(gathererForOperation, operationExtension.getParameters(), "parameter", PARAMETER_RESERVED_NAMES);
      gathererForOperation.getErrors()
          .forEach(error -> gatherer.addError(String.format("%s, for the operation [%s]", error, operationExtension.getName())));
    });
  }

  private void validateParameters(ErrorGatherer gatherer, List<ParameterExtension> parameters, String tagName,
                                  Set<String> reservedNames) {
    parameters.forEach(parameterExtension -> {
      String name = parameterExtension.getName();
      if (reservedNames.contains(name)) {
        gatherer.addError(String.format("The " + tagName + " [%s] is using the a reserved word", name));
      }
      if (!VALID_TAG_NAME.matcher(name).matches()) {
        gatherer.addError(String.format("The " + tagName + " [%s] has invalid characters for an XML attribute", name));
      }
    });

    parameters.stream()
        .collect(Collectors.groupingBy(parameterExtension -> parameterExtension.getName()))
        .forEach((parameterName, parameterExtensions) -> {
          if (parameterExtensions.size() > 1) {
            gatherer.addError(String.format("The " + tagName + " [%s] is repeated [%d] times", parameterName,
                                            parameterExtensions.size()));
          }
        });
  }


  public class ErrorGatherer {

    private List<String> errors = new ArrayList<>();

    public void addError(String errorMessage) {
      errors.add(errorMessage);
    }

    public List<String> getErrors() {
      return errors;
    }
  }
}
