/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.application;

import java.io.IOException;

/**
 * Creates {@link Application} instances
 */
public interface ApplicationFactory
{

    /**
     * Creates an application
     *
     * @param appName the name of the application to create
     * @return the application instance that corresponds to the given name
     * @throws IOException
     */
    //TODO(pablo.kraan): createApp should throw an Exception class more related
    //to the domain instead of a low level IOException
    public Application createApp(String appName) throws IOException;
}
