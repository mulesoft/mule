/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
