/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.LargeTest;

@LargeTest
/**
 * Base test case class for creating a multi-process test case.
 */
public abstract class AbstractMultipleProcessTestCase extends AbstractMuleContextTestCase {

  @Override
  public int getTestTimeoutSecs() {
    return TestUtils.getTimeout(120);
  }

}
