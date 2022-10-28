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
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(APPLICATION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class ErrorsInApplicationExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  private static final Map<String, Set<String>> expectedErrors = new HashMap<>();

  @BeforeClass
  public static void setupExpectedErrors() {
    expectedErrors.put("raiseThis", singleton("THIS:CUSTOM"));
    expectedErrors.put("raiseCustom", singleton("THIS:CUSTOM"));
    expectedErrors.put("heisenbergCureCancer", asSet("HEISENBERG:HEALTH", "HEISENBERG:OAUTH2"));

    expectedErrors.put("withMappingInsideBody", asSet("MY:MAPPED", "HEISENBERG:OAUTH2"));
    expectedErrors.put("mappingAnyInsideBody", singleton("MY:MAPPED"));
    expectedErrors.put("transitiveMapping", asSet("MY:MAPPED", "HEISENBERG:OAUTH2"));
    expectedErrors.put("mappingChildAfterParent", singleton("MY:MAPPEDCONNECTIVITY"));

    // We can't guess what errors will set-payload raise (this operation will raise a "MULE:EXPRESSION" error).
    expectedErrors.put("divisionByZero", emptySet());

    expectedErrors.put("operationSilencingOneSpecificErrorAndRaisingAnother", asSet("HEISENBERG:OAUTH2", "THIS:CUSTOM"));
    expectedErrors.put("operationSilencingAllErrorsAndRaisingAnother", singleton("THIS:CUSTOM"));
    expectedErrors.put("operationSilencingAllHeisenbergErrorsAndRaisingAnother", singleton("THIS:HEALTH"));
    expectedErrors.put("operationSilencingAllHealthErrorsWithinACatchAll", singleton("HEISENBERG:OAUTH2"));

    expectedErrors.put("operationRaisingUniqueErrorAndCatchingIt", emptySet());

    expectedErrors.put("operationWithMultipleOnErrorContinues", emptySet());
    expectedErrors.put("operationCatchingAllButWithWhen", asSet("HEISENBERG:OAUTH2", "HEISENBERG:HEALTH"));
  }

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "mule-error-handling-operations-config.xml",
        "mule-error-handling-with-try-operations-config.xml"
    };
  }

  @Test
  public void appExtensionModelContainsRaisedErrors() {
    ExtensionModel extensionModel = getAppExtensionModel();
    Set<String> raisedErrors = getRaisedErrors(extensionModel);
    assertThat(raisedErrors,
               containsInAnyOrder("THIS:CONNECTIVITY", "MULE:ANY", "MULE:RETRY_EXHAUSTED", "THIS:RETRY_EXHAUSTED",
                                  "MULE:CONNECTIVITY", "HEISENBERG:HEALTH", "HEISENBERG:OAUTH2", "THIS:CUSTOM", "THIS:HEALTH",
                                  "THIS:UNIQUE", "MY:MAPPED", "MY:MAPPEDCONNECTIVITY", "THIS:FOURTH", "THIS:THIRD", "THIS:SECOND",
                                  "THIS:FIRST"));
  }

  @Test
  public void eachOperationDeclaresTheErrorsThatRaises() {
    for (Entry<String, Set<String>> expectedForOperation : expectedErrors.entrySet()) {
      String operationName = expectedForOperation.getKey();
      Set<String> expected = expectedForOperation.getValue();
      assertRaisedErrors(operationName, expected);
    }
  }

  private void assertRaisedErrors(String operationName, Collection<String> expectedSetOfErrors) {
    OperationModel operationModel = getOperationModel(operationName);
    Set<String> actualRaisedErrors = getRaisedErrors(operationModel);

    assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors,
               hasSize(expectedSetOfErrors.size()));
    for (String item : expectedSetOfErrors) {
      assertThat(format("Actual set for '%s': %s", operationName, actualRaisedErrors), actualRaisedErrors, hasItem(item));
    }
  }

  private OperationModel getOperationModel(String operationName) {
    return getAppExtensionModel().getOperationModel(operationName)
        .orElseThrow(() -> new IllegalArgumentException(format("Operation '%s' not found in application's extension model",
                                                               operationName)));
  }

  private static Set<String> getRaisedErrors(ExtensionModel extensionModel) {
    return extensionModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  private static Set<String> getRaisedErrors(OperationModel operationModel) {
    return operationModel.getErrorModels().stream().map(ErrorModel::toString).collect(toSet());
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }

  private static <T> Set<T> asSet(T... a) {
    return stream(a).collect(toSet());
  }
}
