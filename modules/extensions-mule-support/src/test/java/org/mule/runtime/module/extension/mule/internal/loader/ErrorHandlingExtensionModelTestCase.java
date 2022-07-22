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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(APPLICATION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class ErrorHandlingExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  private static final Map<String, List<String>> expectedErrors = new HashMap<>();

  @BeforeClass
  public static void setupExpectedErrors() {
    expectedErrors.put("raiseThis", singletonList("THIS:CUSTOM"));
    expectedErrors.put("raiseOther", singletonList("OTHER:CUSTOM"));
    expectedErrors.put("raiseCustom", singletonList("MY:CUSTOM"));
    expectedErrors.put("heisenbergCureCancer", asList("HEISENBERG:HEALTH", "HEISENBERG:OAUTH2"));

    // Not applying the mappings in the extension model. Otherwise, this would declare "MY:MAPPED".
    expectedErrors.put("withMappingInsideBody", asList("HEISENBERG:HEALTH", "HEISENBERG:OAUTH2"));

    // We can't guess what errors will set-payload raise (this operation will raise a "MULE:EXPRESSION" error).
    expectedErrors.put("divisionByZero", emptyList());
  }

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String getConfigFile() {
    return "mule-error-handling-operations-config.xml";
  }

  @Test
  public void appExtensionModelContainsRaisedErrors() {
    ExtensionModel extensionModel = getAppExtensionModel();
    List<String> raisedErrors = getRaisedErrors(extensionModel);
    assertThat(raisedErrors,
               hasItems("THIS:CONNECTIVITY", "MULE:ANY", "MULE:RETRY_EXHAUSTED", "THIS:RETRY_EXHAUSTED", "MULE:CONNECTIVITY",
                        "HEISENBERG:HEALTH", "HEISENBERG:OAUTH2", "MY:CUSTOM", "OTHER:CUSTOM", "THIS:CUSTOM"));
  }

  @Test
  public void eachOperationDeclaresTheErrorsThatRaises() {
    for (Entry<String, List<String>> expectedForOperation : expectedErrors.entrySet()) {
      String operationName = expectedForOperation.getKey();
      List<String> expected = expectedForOperation.getValue();
      assertRaisedErrors(operationName, expected);
    }
  }

  private void assertRaisedErrors(String operationName, Collection<String> expectedListOfErrors) {
    OperationModel operationModel = getOperationModel(operationName);
    List<String> actualRaisedErrors = getRaisedErrors(operationModel);

    assertThat(format("Actual list for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors,
               hasSize(expectedListOfErrors.size()));
    for (String item : expectedListOfErrors) {
      assertThat(actualRaisedErrors, hasItem(item));
    }
  }

  private OperationModel getOperationModel(String operationName) {
    return getAppExtensionModel().getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in application's extension model",
                                                               operationName)));
  }

  private static List<String> getRaisedErrors(ExtensionModel extensionModel) {
    return extensionModel.getErrorModels().stream().map(ErrorModel::toString).collect(toList());
  }

  private static List<String> getRaisedErrors(OperationModel operationModel) {
    return operationModel.getErrorModels().stream().map(ErrorModel::toString).collect(toList());
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }
}
