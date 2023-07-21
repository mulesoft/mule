/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ApplicationStatusMapperTestCase extends AbstractMuleTestCase {

  @Test
  public void mapsNotInLifeCycle() throws Exception {
    doMappingTest(NotInLifecyclePhase.PHASE_NAME, ApplicationStatus.CREATED);
  }

  @Test
  public void mapsInitialisable() throws Exception {
    doMappingTest(Initialisable.PHASE_NAME, ApplicationStatus.INITIALISED);
  }

  @Test
  public void mapsStoppable() throws Exception {
    doMappingTest(Stoppable.PHASE_NAME, ApplicationStatus.STOPPED);
  }

  @Test
  public void mapsStartable() throws Exception {
    doMappingTest(Startable.PHASE_NAME, ApplicationStatus.STARTED);
  }

  @Test
  public void mapsDisposable() throws Exception {
    doMappingTest(Disposable.PHASE_NAME, ApplicationStatus.DESTROYED);
  }

  @Test(expected = IllegalStateException.class)
  public void throwsErrorMappingUnknownPhase() throws Exception {
    ApplicationStatusMapper.getApplicationStatus("unknown");
  }

  private void doMappingTest(String currentPhaseName, ApplicationStatus expectedApplicationStatus) {
    final ApplicationStatus applicationStatus = ApplicationStatusMapper.getApplicationStatus(currentPhaseName);

    assertThat(applicationStatus, equalTo(expectedApplicationStatus));
  }
}
