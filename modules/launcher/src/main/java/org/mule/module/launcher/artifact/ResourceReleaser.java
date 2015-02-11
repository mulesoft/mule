/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

/**
 * Implementations of this class should take care about resources that may leak memory after application undeployment.
 * Mule ensures to create an instance of this class with the same class loader that loaded the application resources
 * in order to ensure the access to them.
 */
public interface ResourceReleaser
{

    /**
     * This method will be called as part of the application undeployment.
     * No exceptions should be thrown due to any issues while cleaning resources are not related to the application
     * itself but with resource housekeeping.
     */
    void release();
}
