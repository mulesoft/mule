package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.descriptor.DescriptorParser;
import org.mule.module.launcher.descriptor.EmptyApplicationDescriptor;
import org.mule.module.launcher.descriptor.PropertiesDescriptorParser;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 */
public class DefaultAppBloodhound implements AppBloodhound
{

    protected Map<String, DescriptorParser> parserRegistry;

    public DefaultAppBloodhound()
    {
        parserRegistry = new HashMap<String, DescriptorParser>();
        // file extension -> parser implementation
        // TODO MULE-4909 better spi discovery mechanism with weighs
        parserRegistry.put("properties", new PropertiesDescriptorParser());
    }

    public ApplicationDescriptor fetch(String appName) throws IOException
    {
        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

        File appDir = new File(String.format("%s/apps/%s", muleHome, appName));
        // list mule-deploy.* files
        @SuppressWarnings("unchecked")
        Collection<File> deployFiles = FileUtils.listFiles(appDir, new WildcardFileFilter("mule-deploy.*"), null);

        // none found, return defaults
        if (deployFiles.isEmpty())
        {
            return new EmptyApplicationDescriptor(appName);
        }

        // lookup the implementation by extension

        for (File file : deployFiles)
        {
            final String ext = FilenameUtils.getExtension(file.getName());
            System.out.println("ext = " + ext);
        }

        final String deployConfig = String.format("%s/apps/%s/%s", muleHome, appName, "mule-deploy.properties");

        ApplicationDescriptor desc = new PropertiesDescriptorParser().parse(new File(deployConfig));
        // app name is external to the deployment descriptor
        desc.setAppName(appName);
        return desc;

    }
}
