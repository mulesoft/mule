/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.core.util.pool;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

public abstract class AbstractPoolingTestCase extends AbstractMuleContextTestCase {

  protected static final int DEFAULT_EXHAUSTED_ACTION = PoolingProfile.WHEN_EXHAUSTED_FAIL;
  protected static final int DEFAULT_INITIALISATION_POLICY = PoolingProfile.INITIALISE_NONE;
  protected static final int MAX_ACTIVE = 3;
  protected static final int MAX_IDLE = -1;
  protected static final long MAX_WAIT = 1500;

  protected PoolingProfile createDefaultPoolingProfile() {
    PoolingProfile poolingProfile = new PoolingProfile();
    poolingProfile.setExhaustedAction(DEFAULT_EXHAUSTED_ACTION);
    poolingProfile.setInitialisationPolicy(DEFAULT_INITIALISATION_POLICY);
    poolingProfile.setMaxActive(MAX_ACTIVE);
    poolingProfile.setMaxIdle(MAX_IDLE);
    poolingProfile.setMaxWait(MAX_WAIT);
    return poolingProfile;
  }
}
