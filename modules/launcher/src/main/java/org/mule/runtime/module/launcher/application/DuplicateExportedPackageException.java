/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

/**
 * Thrown to indicate that more than one artifact is exporting a given package.
 */
public class DuplicateExportedPackageException extends ArtifactDescriptorCreateException
{

    /**
     * {@inheritDoc}
     */
    public DuplicateExportedPackageException(String message)
    {
        super(message);
    }
}
