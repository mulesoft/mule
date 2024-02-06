/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.tck.junit4.rule.SystemProperty.callWithProperty;
import static org.mule.test.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;

import static java.util.Collections.singletonList;

import org.mule.test.oauth.TestOAuthExtension;

import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized;

public class OCSExtensionModelJsonGeneratorTestCase extends ExtensionModelJsonGeneratorTestCase {

  /**
   * Property that if set signals that OCS is supported.
   */
  public static final String OCS_ENABLED = "ocs.enabled";

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
