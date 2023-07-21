/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.SupportedJavaVersions.ENFORCE_EXTENSION_JAVA_VERSION;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

@SmallTest
@Feature(SDK)
@Story(ENFORCE_EXTENSION_JAVA_VERSION)
public class StrictExtensionJdkValidatorTestCase extends BaseExtensionJdkValidatorTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected ExtensionJdkValidator validatorFor(JdkVersionUtils.JdkVersion jdkVersion) {
    return new StrictExtensionJdkValidator(jdkVersion);
  }

  @Override
  protected void assertFailure(Runnable task) {
    expectedException.expect(JavaVersionNotSupportedByExtensionException.class);
    task.run();
  }
}
