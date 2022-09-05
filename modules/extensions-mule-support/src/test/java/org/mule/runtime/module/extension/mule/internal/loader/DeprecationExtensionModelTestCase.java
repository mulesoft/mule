/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(APPLICATION_EXTENSION_MODEL)
public class DeprecationExtensionModelTestCase extends AbstractMuleSdkExtensionModelTestCase {

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
}
