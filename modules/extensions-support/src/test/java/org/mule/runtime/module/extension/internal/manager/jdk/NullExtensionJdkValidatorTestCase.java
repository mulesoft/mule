/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;
import org.mule.tck.size.SmallTest;

@SmallTest
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
