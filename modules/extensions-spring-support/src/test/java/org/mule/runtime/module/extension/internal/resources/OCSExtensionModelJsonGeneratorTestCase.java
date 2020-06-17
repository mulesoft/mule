/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ENABLED;
import static org.mule.runtime.module.extension.internal.resources.ExtensionModelJsonGeneratorTestCase.ExtensionJsonGeneratorTestUnit.newTestUnit;
import static org.mule.tck.junit4.rule.SystemProperty.callWithProperty;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.test.oauth.TestOAuthExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.runners.Parameterized;

public class OCSExtensionModelJsonGeneratorTestCase extends ExtensionModelJsonGeneratorTestCase {

  private static ExtensionModelLoader javaLoader = new DefaultJavaExtensionModelLoader();

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ExtensionJsonGeneratorTestUnit> extensions =
        Collections.singletonList(newTestUnit(javaLoader, TestOAuthExtension.class, "test-oauth-ocs.json"));
    try {
      return callWithProperty(OCS_ENABLED, "true", () -> createExtensionModels(extensions));
    } catch (Throwable t) {
      throw new RuntimeException("Failed to create the extension models for the test.", t);
    }
  }
}
