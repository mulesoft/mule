/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Collections.singletonList;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ENABLED;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;
import static org.mule.tck.junit4.rule.SystemProperty.callWithProperty;

import org.mule.test.oauth.TestOAuthExtension;

import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized;

public class OCSExtensionModelJsonGeneratorTestCase extends ExtensionModelJsonGeneratorTestCase {

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ResourceExtensionUnitTest> extensions =
        singletonList(newUnitTest(JAVA_LOADER, TestOAuthExtension.class, "test-oauth-ocs.json"));
    try {
      return callWithProperty(OCS_ENABLED, "true", () -> createExtensionModels(extensions));
    } catch (Throwable t) {
      throw new RuntimeException("Failed to create the extension models for the test.", t);
    }
  }
}
