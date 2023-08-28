/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.performance.util;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;

public abstract class AbstractIsolatedFunctionalPerformanceTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

}
