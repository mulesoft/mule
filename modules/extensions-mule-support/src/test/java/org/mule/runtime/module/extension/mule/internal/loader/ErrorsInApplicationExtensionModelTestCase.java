/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.asSet;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.assertRaisedErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.getRaisedErrors;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

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

    expectedErrors.put("operationSilencingOneSpecificErrorAndRaisingAnother",
                       asSet("HEISENBERG:OAUTH2", "THIS:CUSTOM", "MULE:TRANSACTION"));
    expectedErrors.put("operationSilencingAllErrorsAndRaisingAnother", asSet("THIS:CUSTOM", "MULE:TRANSACTION"));
    expectedErrors.put("operationSilencingAllHeisenbergErrorsAndRaisingAnother", asSet("THIS:HEALTH", "MULE:TRANSACTION"));
    expectedErrors.put("operationSilencingAllHealthErrorsWithinACatchAll", asSet("HEISENBERG:OAUTH2", "MULE:TRANSACTION"));

    expectedErrors.put("operationRaisingUniqueErrorAndCatchingIt", singleton("MULE:TRANSACTION"));

    expectedErrors.put("operationWithMultipleOnErrorContinues", singleton("MULE:TRANSACTION"));
    expectedErrors.put("operationCatchingAllButWithWhen", asSet("HEISENBERG:OAUTH2", "HEISENBERG:HEALTH", "MULE:TRANSACTION"));
  }

  @Inject
  private ExtensionManager extensionManager;

  private ExtensionModel appExtensionModel;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "mule-error-handling-operations-config.xml",
        "mule-error-handling-with-try-operations-config.xml"
    };
  }

  @Before
  public void setUp() {
    appExtensionModel = getAppExtensionModel();
  }

  @Test
  public void appExtensionModelContainsRaisedErrors() {
    Set<String> raisedErrors = getRaisedErrors(appExtensionModel);
    assertThat(raisedErrors,
               containsInAnyOrder("THIS:CONNECTIVITY", "MULE:ANY", "MULE:RETRY_EXHAUSTED", "THIS:RETRY_EXHAUSTED",
                                  "MULE:CONNECTIVITY", "HEISENBERG:HEALTH", "HEISENBERG:OAUTH2", "THIS:CUSTOM", "THIS:HEALTH",
                                  "THIS:UNIQUE", "MY:MAPPED", "MY:MAPPEDCONNECTIVITY", "THIS:FOURTH", "THIS:THIRD", "THIS:SECOND",
                                  "THIS:FIRST", "MULE:TRANSACTION"));
  }

  @Test
  public void eachOperationDeclaresTheErrorsThatRaises() {
    for (Entry<String, Set<String>> expectedForOperation : expectedErrors.entrySet()) {
      String operationName = expectedForOperation.getKey();
      Set<String> expected = expectedForOperation.getValue();
      assertRaisedErrors(appExtensionModel, operationName, expected);
    }
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }
}
