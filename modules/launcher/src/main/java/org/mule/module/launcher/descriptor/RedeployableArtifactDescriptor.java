/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import org.mule.module.artifact.descriptor.ArtifactDescriptor;

public class RedeployableArtifactDescriptor extends ArtifactDescriptor
{
    public static final String DEFAULT_DEPLOY_PROPERTIES_RESOURCE = "mule-deploy.properties";

    private boolean redeploymentEnabled = true;

    public boolean isRedeploymentEnabled()
    {
        return redeploymentEnabled;
    }

    public void setRedeploymentEnabled(boolean redeploymentEnabled)
    {
        this.redeploymentEnabled = redeploymentEnabled;
    }
}
