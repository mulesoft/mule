/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.SupportedJavaVersions.ENFORCE_EXTENSION_JAVA_VERSION;

import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(SDK)
@Story(ENFORCE_EXTENSION_JAVA_VERSION)
public class NullExtensionJdkValidatorTestCase extends BaseExtensionJdkValidatorTestCase {

  @Override
  protected ExtensionJdkValidator validatorFor(JdkVersion jdkVersion) {
    return new NullExtensionJdkValidator(jdkVersion);
  }

  @Override
  protected void assertFailure(Runnable task) {
    // expect no failure
    task.run();
  }
}
