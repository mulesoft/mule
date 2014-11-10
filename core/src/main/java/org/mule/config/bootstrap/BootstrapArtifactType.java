/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.bootstrap;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;

public enum BootstrapArtifactType
{
    APP("app"), DOMAIN("domain"), ALL("app/domain");

    public static final String APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY = "applyToArtifactType";
    private final String artifactTypeAsString;

    BootstrapArtifactType(String artifactTypeAsString)
    {
        this.artifactTypeAsString = artifactTypeAsString;
    }

    public String getAsString()
    {
        return this.artifactTypeAsString;
    }

    public static BootstrapArtifactType createFromString(String artifactTypeAsString)
    {
        for (BootstrapArtifactType bootstrapArtifactType : values())
        {
            if (bootstrapArtifactType.artifactTypeAsString.equals(artifactTypeAsString))
            {
                return bootstrapArtifactType;
            }
        }
        throw new MuleRuntimeException(CoreMessages.createStaticMessage("No artifact type found for value: " + artifactTypeAsString));
    }
}
