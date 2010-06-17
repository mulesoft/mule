package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
public class DefaultAppBloodhound implements AppBloodhound
{

    public ApplicationDescriptor fetch(String appName) throws IOException
    {
        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
        // TODO pluggable discovery mechanism
        final String deployConfig = String.format("%s/apps/%s/%s", muleHome, appName, "mule-deploy.properties");

        final Properties p = new Properties();
        final InputStream is = IOUtils.getResourceAsStream(deployConfig, getClass());
        if (is == null)
        {
            return new EmptyApplicationDescriptor();
        }

        p.load(is);

        return new ApplicationDescriptor()
        {
            public String getEncoding()
            {
                return p.getProperty("encoding");
            }

            public String getConfigurationBuilder()
            {
                return p.getProperty("config.builder");
            }

            public String getDomainName()
            {
                return p.getProperty("domain");
            }

            public boolean isParentFirstClassLoader()
            {
                return Boolean.parseBoolean(p.getProperty("classloader.parentFirst", Boolean.TRUE.toString()));
            }

            public int getDescriptorVersion()
            {
                return 1;
            }
        };
    }
}
