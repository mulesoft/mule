/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.performance.util;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;

public abstract class AbstractIsolatedFunctionalPerformanceTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

}
