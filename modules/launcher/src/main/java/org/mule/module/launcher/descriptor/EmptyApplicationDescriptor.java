package org.mule.module.launcher.descriptor;

/**
 * Encapsulates defaults when no explicit descriptor provided with an app.
 */
public class EmptyApplicationDescriptor extends ApplicationDescriptor
{

    private String appName;

    public EmptyApplicationDescriptor(String appName)
    {
        this.appName = appName;
    }

    public String getAppName()
    {
        return appName;
    }

}
