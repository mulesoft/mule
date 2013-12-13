/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.lifecycle.phases.NotInLifecyclePhase;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps an application life cycle phase to an application status
 */
public class ApplicationStatusMapper
{

    private static Map<String, ApplicationStatus> statusMapping;

    public static ApplicationStatus getApplicationStatus(String currentPhase)
    {
        final ApplicationStatus applicationStatus = getStatusMapping().get(currentPhase);

        if (applicationStatus == null)
        {
            throw new IllegalStateException("Unknown lifecycle phase: " + currentPhase);
        }

        return applicationStatus;
    }

    private static Map<String, ApplicationStatus> getStatusMapping()
    {
        if (statusMapping == null)
        {
            synchronized (ApplicationStatusMapper.class)
            {
                if (statusMapping == null)
                {
                    statusMapping = new HashMap<String, ApplicationStatus>();

                    statusMapping.put(NotInLifecyclePhase.PHASE_NAME, ApplicationStatus.CREATED);
                    statusMapping.put(Disposable.PHASE_NAME, ApplicationStatus.DESTROYED);
                    statusMapping.put(Stoppable.PHASE_NAME, ApplicationStatus.STOPPED);
                    statusMapping.put(Startable.PHASE_NAME, ApplicationStatus.STARTED);
                    statusMapping.put(Initialisable.PHASE_NAME, ApplicationStatus.INITIALISED);
                }
            }
        }

        return statusMapping;
    }
}