/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.coreextension;

import org.mule.MuleCoreExtension;

/**
 * Thrown to indicate that a mule core extension has an illegal dependency
 * against another {@link MuleCoreExtension}.
 */public class IllegalDependencyException extends RuntimeException
{

    public IllegalDependencyException(String message)
    {
        super(message);
    }
}
