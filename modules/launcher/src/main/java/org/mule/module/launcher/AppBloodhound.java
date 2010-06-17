package org.mule.module.launcher;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 * Discovers application descriptor and settings.
 */
public interface AppBloodhound
{
    ApplicationDescriptor fetch(String appName) throws IOException;
}
