/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;

import static java.lang.String.format;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import jakarta.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(APPLICATION_EXTENSION_MODEL)
public class AbstractMuleSdkExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  protected ParameterModel getParameterModel(OperationModel operationModel, String parameterName) {
    return operationModel.getAllParameterModels().stream().filter(pm -> pm.getName().equals(parameterName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Parameter '%s' not found in the operation model of '%s'",
                                                               parameterName, operationModel.getName())));
  }

  protected OperationModel getOperationModel(ExtensionModel extensionModel, String operationName) {
    return extensionModel.getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in application's extension model",
                                                               operationName)));
  }

  protected ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }
}
