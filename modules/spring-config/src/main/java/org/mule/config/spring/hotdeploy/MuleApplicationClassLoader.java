package org.mule.config.spring.hotdeploy;

import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleApplicationClassLoader extends URLClassLoader
{

    protected static final URL[] CLASSPATH_EMPTY = new URL[0];
    protected final transient Log logger = LogFactory.getLog(getClass());
    private File monitoredResource;

    public MuleApplicationClassLoader(File monitoredResource, ClassLoader parentCl)
    {
        super(CLASSPATH_EMPTY, parentCl);
        try
        {
            this.monitoredResource = monitoredResource;
            // get lib dir on the same level as monitored resource and...
            File parentFile = monitoredResource.getParentFile();
            File libDir = new File(parentFile, "lib");

            if (logger.isInfoEnabled())
            {
                logger.info("Library directory: " + libDir);
            }

            URL[] urls = ReloadableBuilder.CLASSPATH_EMPTY;

            if (libDir.exists() && libDir.canRead())
            {
                Collection jars = FileUtils.listFiles(libDir, new String[] {"jar"}, false);

                File[] jarFiles = (File[]) jars.toArray(new File[jars.size()]);

                urls = FileUtils.toURLs(jarFiles);

                if (urls.length > 0 && logger.isInfoEnabled())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Updating the following jars:").append(SystemUtils.LINE_SEPARATOR);
                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);
                    for (URL url : urls)
                    {
                        sb.append(url).append(SystemUtils.LINE_SEPARATOR);
                    }
                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    logger.info(sb.toString());
                }
            }

            for (URL url : urls)
            {
                addURL(url);

            }
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
    }

    public File getMonitoredResource()
    {
        return this.monitoredResource;
    }
}