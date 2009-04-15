package org.mule.config.spring.hotdeploy;

/**
 *
 */
public class DefaultMuleSharedDomainClassLoader extends MuleSharedDomainClassLoader
{

    protected static final String DEFAULT_DOMAIN_NAME = "default";

    public DefaultMuleSharedDomainClassLoader(ClassLoader parent)
    {
        super(DEFAULT_DOMAIN_NAME, parent);
    }
}
