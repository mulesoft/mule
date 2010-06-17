package org.mule.module.launcher.descriptor;

/**
 * Encapsulates defaults when no explicit descriptor provided with an app.
 */
public class EmptyApplicationDescriptor extends ApplicationDescriptor
{

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
