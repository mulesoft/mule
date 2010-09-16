package org.mule.module.launcher;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 *
 */
public class ApplicationFactory
{
    public static Application createApp(String appName) throws IOException
    {
        AppBloodhound bh = new DefaultAppBloodhound();
        final ApplicationDescriptor descriptor = bh.fetch(appName);
        if (descriptor.isPriviledged())
        {
            // TODO implement
            //delegate = new PriviledgedMuleApplication(appName);
            return new ApplicationWrapper(new DefaultMuleApplication(appName));
        }
        else
        {
            return new ApplicationWrapper(new DefaultMuleApplication(appName));
        }
    }
}
