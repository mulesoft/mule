/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_DEFAULT_THREADING_PROFILE;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.config.ThreadingProfile;

import org.junit.Test;

public class DefaultThreadingProfileTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[0];
  }

  @Test
  public void testDefaultThreadingProfile() {
    final ThreadingProfile defaultThreadingProfile = muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_THREADING_PROFILE);
    assertNotNull(defaultThreadingProfile);
    assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, defaultThreadingProfile.getMaxThreadsActive());
    assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_IDLE, defaultThreadingProfile.getMaxThreadsIdle());
    assertEquals(ThreadingProfile.DEFAULT_MAX_THREAD_TTL, defaultThreadingProfile.getThreadTTL());
    assertEquals(ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE, defaultThreadingProfile.getMaxBufferSize());
    assertEquals(ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION, defaultThreadingProfile.getPoolExhaustedAction());
    assertEquals(ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, defaultThreadingProfile.getThreadWaitTimeout());
    assertEquals(ThreadingProfile.DEFAULT_DO_THREADING, defaultThreadingProfile.isDoThreading());
  }
}
