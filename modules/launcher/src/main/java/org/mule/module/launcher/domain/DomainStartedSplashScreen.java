/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.mule.module.launcher.MuleFoldersUtil.getDomainLibFolder;
import org.mule.module.launcher.artifact.ArtifactStartedSplashScreen;
import org.mule.module.launcher.descriptor.DomainDescriptor;

public class DomainStartedSplashScreen extends ArtifactStartedSplashScreen
{
    public void doBody(DomainDescriptor descriptor)
    {
        doBody(String.format("Started domain '%s'", descriptor.getName()));
        if (RUNTIME_VERBOSE_PROPERTY.isEnabled())
        {
            listLibraries(descriptor);
            listOverrides(descriptor);
        }
    }

    protected void listLibraries(DomainDescriptor descriptor)
    {
        listItems(getLibraries(getDomainLibFolder(descriptor.getName())), "Domain libraries: ");
    }
}
