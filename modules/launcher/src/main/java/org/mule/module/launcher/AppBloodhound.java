/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 * Discovers application descriptor and settings.
 */
public interface AppBloodhound
{

    /**
     * Sniff around and get me the app descriptor!
     */
    ApplicationDescriptor fetch(String appName) throws IOException;
}
