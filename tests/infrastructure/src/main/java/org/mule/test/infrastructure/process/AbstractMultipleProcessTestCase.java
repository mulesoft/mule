/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
