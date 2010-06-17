package org.mule.module.launcher;

import java.io.IOException;

/**
 * Discovers application descriptor and settings.
 */
public interface AppBloodhound
{
    ApplicationDescriptor fetch(String appName) throws IOException;
}
