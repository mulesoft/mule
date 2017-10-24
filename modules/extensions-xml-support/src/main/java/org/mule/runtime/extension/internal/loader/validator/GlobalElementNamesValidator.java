/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validator;

import static java.lang.String.format;
import static org.mule.runtime.internal.util.NameValidationUtil.verifyStringDoesNotContainsReservedCharacters;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which are XML based. It validates global element names
 * are properly written, and there's no name clashing between them.
 *
 * @since 4.0
 */
public class GlobalElementNamesValidator implements ExtensionModelValidator {

  public static final String REPEATED_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE =
      "Two configuration elements have been defined with the same global name. Global name [%s] must be unique. Clashing components are %s and %s";

  public static final String ILLEGAL_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE =
      "Global name \"%s\" is ilegal. %s";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      Map<String, ComponentModel> existingObjectsWithName = new HashMap<>();

      @Override
      protected void onConfiguration(ConfigurationModel configurationModel) {
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
            .ifPresent(globalElementComponentModelModelProperty -> {
              globalElementComponentModelModelProperty.getGlobalElements().stream()
                  .filter(componentModel -> componentModel.getNameAttribute() != null)
                  .forEach(componentModel -> {
                    String nameAttributeValue = componentModel.getNameAttribute();
                    validateDuplicatedGlobalElements(configurationModel, componentModel, nameAttributeValue,
                                                     existingObjectsWithName, problemsReporter);
                    validateNotReservedCharacterInName(configurationModel, nameAttributeValue, problemsReporter);
                  });
            });
      }
    }.walk(extensionModel);
  }

  private void validateNotReservedCharacterInName(ConfigurationModel configurationModel, String nameAttributeValue,
                                                  ProblemsReporter problemsReporter) {
    try {
      verifyStringDoesNotContainsReservedCharacters(nameAttributeValue);
    } catch (IllegalArgumentException e) {
      problemsReporter
          .addError(new Problem(configurationModel, format(ILLEGAL_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE, nameAttributeValue,
                                                           StringUtils.isBlank(e.getMessage()) ? "" : e.getMessage())));
    }
  }

  private void validateDuplicatedGlobalElements(ConfigurationModel configurationModel, ComponentModel componentModel,
                                                String nameAttributeValue, Map<String, ComponentModel> existingObjectsWithName,
                                                ProblemsReporter problemsReporter) {
    if (existingObjectsWithName.containsKey(nameAttributeValue)) {
      problemsReporter.addError(new Problem(configurationModel, format(
                                                                       REPEATED_GLOBAL_ELEMENT_NAME_FORMAT_MESSAGE,
                                                                       nameAttributeValue,
                                                                       existingObjectsWithName.get(nameAttributeValue)
                                                                           .getIdentifier(),
                                                                       componentModel.getIdentifier())));
    } else {
      existingObjectsWithName.put(nameAttributeValue, componentModel);
    }
  }
}
