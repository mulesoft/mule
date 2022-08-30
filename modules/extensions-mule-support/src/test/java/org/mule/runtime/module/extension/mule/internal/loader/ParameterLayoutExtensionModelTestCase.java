/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.meta.model.display.PathModel.Location.EMBEDDED;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(APPLICATION_EXTENSION_MODEL)
public class ParameterLayoutExtensionModelTestCase extends AbstractMuleSdkExtensionModelTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-parameter-layout-metadata-config.xml";
  }

  @Test
  public void fullConfiguredParameterLayout() {
    ExtensionModel extensionModel = getAppExtensionModel();
    OperationModel operationModel = getOperationModel(extensionModel, "fullParameterLayoutOperation");
    ParameterModel parameterModel = getParameterModel(operationModel, "theParameter");

    DisplayModel displayModel = parameterModel.getDisplayModel().get();

    assertThat(displayModel.getDisplayName(), is("The display name"));
    assertThat(displayModel.getSummary(), is("The summary"));
    assertThat(displayModel.getExample(), is("The example"));

    PathModel pathModel = displayModel.getPathModel().get();

    assertThat(pathModel.getType(), is(FILE));
    assertThat(pathModel.acceptsUrls(), is(false));
    assertThat(pathModel.getLocation(), is(EMBEDDED));
    assertThat(pathModel.getFileExtensions(), containsInAnyOrder("txt", "xml", "png"));

    LayoutModel layoutModel = parameterModel.getLayoutModel().get();

    assertThat(layoutModel.getOrder().get(), is(4));
    assertThat(layoutModel.isText(), is(true));

    // These things aren't configurable, and they won't
    assertThat(displayModel.getClassValueModel().isPresent(), is(false));
    assertThat(layoutModel.getTabName().isPresent(), is(false));
  }
}
