package org.mule.config.bootstrap;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Allows to configure a SimpleRegistryBootstrap using a single Properties object.
 */
public class SinglePropertiesRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer
{

    private final Properties properties;

    public SinglePropertiesRegistryBootstrapDiscoverer(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public List<Properties> discover()
    {
        return Arrays.asList(properties);
    }
}
