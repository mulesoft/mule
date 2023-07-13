/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

@SmallTest
public class StrictExtensionJdkValidatorTestCase extends BaseExtensionJdkValidatorTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected ExtensionJdkValidator validatorFor(JdkVersionUtils.JdkVersion jdkVersion) {
    return new StrictExtensionJdkValidator(jdkVersion);
  }

  @Override
  protected void assertFailure(Runnable task) {
    expectedException.expect(IllegalArgumentException.class);
    task.run();
  }
}
