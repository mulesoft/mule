/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.List;

/**
 * Validates that no {@link ParameterModel parameters} named {@code target}, since that word is reserved.
 *
 * @since 4.0
 */
public final class OperationParametersModelValidator implements ModelValidator {

  private final List<String> reservedWords = ImmutableList.of(TARGET_ATTRIBUTE);

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    Multimap<String, String> offenses = LinkedHashMultimap.create();
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        collectOffenses(offenses, model);
      }
    }.walk(extensionModel);


    if (!offenses.isEmpty()) {
      StringBuilder message =
          new StringBuilder(format("Extension '%s' defines operations which have parameters named after reserved words. Offending operations are:\n",
                                   extensionModel.getName()));

      offenses.asMap().forEach((key, values) -> message.append(format("%s: [%s]", key, Joiner.on(", ").join(values))));

      throw new IllegalOperationModelDefinitionException(message.toString());
    }
  }

  private void collectOffenses(Multimap<String, String> offenses, OperationModel operationModel) {
    operationModel.getParameterModels().stream().filter(parameter -> reservedWords.contains(parameter.getName()))
        .forEach(parameter -> offenses.put(parameter.getName(), operationModel.getName()));
  }
}
