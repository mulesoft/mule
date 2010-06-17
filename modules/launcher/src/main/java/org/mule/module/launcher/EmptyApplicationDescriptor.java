package org.mule.module.launcher;

/**
 * Encapsulates defaults when no explicit descriptor provided with an app.
 */
public class EmptyApplicationDescriptor implements ApplicationDescriptor
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
}
