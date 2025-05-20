/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth.TestOAuthExtension;

import java.util.Collection;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OCSExtensionModelJsonGeneratorTestCase extends ExtensionModelJsonGeneratorTestCase {

  protected static final ExtensionModelLoader JAVA_LOADER = new DefaultJavaExtensionModelLoader();

  /**
   * Property that if set signals that OCS is supported.
   */
  public static final String OCS_ENABLED = "ocs.enabled";

  @Rule
  public SystemProperty ocsEnabled = new SystemProperty(OCS_ENABLED, "true");

  @Parameterized.Parameters(name = "{2}")
  public static Collection<Object[]> data() {
    return singletonList(new Object[] {JAVA_LOADER, TestOAuthExtension.class, "test-oauth-ocs.json",
        null, emptyList()});
  }

}
