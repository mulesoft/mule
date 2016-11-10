/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.application;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps an application life cycle phase to an application status
 */
public class ApplicationStatusMapper {

  private static final Map<String, ApplicationStatus> statusMapping = getStatusMapping();

  public static ApplicationStatus getApplicationStatus(String currentPhase) {
    final ApplicationStatus applicationStatus = statusMapping.get(currentPhase);

    if (applicationStatus == null) {
      throw new IllegalStateException("Unknown lifecycle phase: " + currentPhase);
    }

    return applicationStatus;
  }

  private static Map<String, ApplicationStatus> getStatusMapping() {
    Map<String, ApplicationStatus> statusMapping = new HashMap<>();

    statusMapping.put(NotInLifecyclePhase.PHASE_NAME, ApplicationStatus.CREATED);
    statusMapping.put(Disposable.PHASE_NAME, ApplicationStatus.DESTROYED);
    statusMapping.put(Stoppable.PHASE_NAME, ApplicationStatus.STOPPED);
    statusMapping.put(Startable.PHASE_NAME, ApplicationStatus.STARTED);
    statusMapping.put(Initialisable.PHASE_NAME, ApplicationStatus.INITIALISED);

    return statusMapping;
  }
}
