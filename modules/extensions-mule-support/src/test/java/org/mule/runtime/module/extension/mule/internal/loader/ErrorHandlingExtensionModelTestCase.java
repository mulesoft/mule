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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(APPLICATION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class ErrorHandlingExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String getConfigFile() {
    return "mule-error-handling-operations-config.xml";
  }

  @Test
  public void appExtensionModelContainsRaisedErrors() throws Exception {
    ExtensionModel extensionModel = getAppExtensionModel();
    assertThat(extensionModel.getErrorModels().stream().map(em -> em.getType()).collect(toList()), hasItem("CUSTOM"));
    assertThat(extensionModel.getErrorModels().stream().map(em -> em.toString()).collect(toList()), hasItem("OTHER:CUSTOM"));
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }
}
