/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.api.MuleContext;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;

public interface Application extends Artifact
{

    MuleContext getMuleContext();

    ApplicationDescriptor getDescriptor();

    /**
     * @return the domain associated with the application.
     */
    Domain getDomain();

    /**
     * @return the current status of the application
     */
    ApplicationStatus getStatus();

}
