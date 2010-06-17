package org.mule.module.launcher.descriptor;

import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class PropertiesDescriptorParser implements DescriptorParser
{

    public ApplicationDescriptor parse(File descriptor) throws IOException
    {
        final Properties p = new Properties();
        p.load(new FileReader(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty("encoding"));
        d.setConfigurationBuilder(p.getProperty("config.builder"));
        d.setDomain(p.getProperty("domain"));
        d.setParentFirstClassLoader(Boolean.parseBoolean(p.getProperty("classloader.parentFirst", Boolean.TRUE.toString())));

        final String urlsProp = p.getProperty("config.urls");
        String[] urls;
        if (StringUtils.isBlank(urlsProp))
        {
            urls = new String[] {ApplicationDescriptor.DEFAULT_CONFIGURATION_URL};
        }
        else
        {
            urls = urlsProp.split(",");
        }
        d.setConfigUrls(urls);

        return d;
    }
}
