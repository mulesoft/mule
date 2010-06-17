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

    public String getEncoding()
    {
        return null;
    }

    public String getConfigurationBuilder()
    {
        return null;
    }

    public String getDomainName()
    {
        return null;
    }

    public boolean isParentFirstClassLoader()
    {
        return true;
    }

    public int getDescriptorVersion()
    {
        return 1;
    }

    public String[] getConfigUrls()
    {
        return new String[] {DEFAULT_CONFIGURATION_URL};
    }
}
