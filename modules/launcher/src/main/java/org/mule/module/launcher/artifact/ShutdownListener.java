/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

/**
 * Optional hook, invoked synchronously right before the class loader is disposed and closed.
 */
public interface ShutdownListener
{

    void execute();
}
