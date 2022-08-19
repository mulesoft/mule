/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(REUSE)
@Story(APPLICATION_EXTENSION_MODEL)
public class DeprecationExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String getConfigFile() {
    return "mule-deprecations-config.xml";
  }

  @Test
  public void operationDeprecationModel() {
    ExtensionModel extensionModel = getAppExtensionModel();
    OperationModel operationModel = getOperationModel(extensionModel, "deprecatedOperation");
    Optional<DeprecationModel> deprecationModel = operationModel.getDeprecationModel();
    assertThat(deprecationModel.isPresent(), is(true));
  }

  @Test
  public void operationParameterDeprecationModel() {
    ExtensionModel extensionModel = getAppExtensionModel();
    OperationModel operationModel = getOperationModel(extensionModel, "operationWithDeprecatedParameter");
    ParameterModel deprecatedParameterModel = getParameterModel(operationModel, "deprecatedParameter");
    Optional<DeprecationModel> deprecationModel = deprecatedParameterModel.getDeprecationModel();
    assertThat(deprecationModel.isPresent(), is(true));
  }

  private ParameterModel getParameterModel(OperationModel operationModel, String parameterName) {
    return operationModel.getAllParameterModels().stream().filter(pm -> pm.getName().equals(parameterName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Parameter '%s' not found in the operation model of '%s'",
                                                               parameterName, operationModel.getName())));
  }

  private OperationModel getOperationModel(ExtensionModel extensionModel, String operationName) {
    return extensionModel.getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in application's extension model",
                                                               operationName)));
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }
}
