/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.asSet;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.assertRaisedErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.getRaisedErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.loadJavaSdkExtension;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.loadMuleSdkExtension;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(EXTENSION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class ErrorsInMuleSdkPluginExtensionModelTestCase extends AbstractMuleSdkAstTestCase {

  private static final Map<String, Set<String>> expectedErrors = new HashMap<>();

  @BeforeClass
  public static void setUp() {
    expectedErrors.put("raiseCustom", singleton("ERRORS:CUSTOM"));
    expectedErrors.put("raiseOther", singleton("ERRORS:OTHER"));
    expectedErrors.put("silencingOneAndRaisingOther", singleton("ERRORS:OTHER"));

    // TODO (W-11471782): should be "ERRORS:HEISENBERG_HEALTH" and "ERRORS:HEISENBERG_OAUTH2"
    expectedErrors.put("heisenbergCureCancer", asSet("HEISENBERG:HEALTH", "HEISENBERG:OAUTH2"));

    addDependencyExtension(loadJavaSdkExtension(HeisenbergExtension.class, emptySet()));
  }

  @Override
  protected String getConfigFile() {
    return null;
  }

  @Test
  public void allErrorsOnExtensionModel() {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-with-errors.xml");
    Set<String> errorsAsString = getRaisedErrors(extensionModel);
    MatcherAssert.assertThat(errorsAsString,
                             containsInAnyOrder("MULE:ANY", "MULE:RETRY_EXHAUSTED", "ERRORS:CUSTOM", "ERRORS:CONNECTIVITY",
                                                "ERRORS:RETRY_EXHAUSTED", "MULE:CONNECTIVITY", "ERRORS:ONE", "ERRORS:OTHER",
                                                // TODO (W-11471782): should be "ERRORS:HEISENBERG_HEALTH" and
                                                // "ERRORS:HEISENBERG_OAUTH2"
                                                "HEISENBERG:HEALTH", "HEISENBERG:OAUTH2"));
  }

  @Test
  public void eachOperationDeclaresTheErrorsThatRaises() {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-with-errors.xml");
    for (Entry<String, Set<String>> expectedForOperation : expectedErrors.entrySet()) {
      String operationName = expectedForOperation.getKey();
      Set<String> expected = expectedForOperation.getValue();
      assertRaisedErrors(extensionModel, operationName, expected);
    }
  }

  private ExtensionModel getExtensionModelFrom(String extensionFile) {
    return loadMuleSdkExtension(extensionFile, this.getClass().getClassLoader(), astParserExtensionModels);
  }
}
