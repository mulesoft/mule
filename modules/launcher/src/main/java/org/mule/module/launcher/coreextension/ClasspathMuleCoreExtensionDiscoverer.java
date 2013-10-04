/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import org.mule.MuleCoreExtension;
import org.mule.api.DefaultMuleException;
import org.mule.util.ClassUtils;

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Discovers {@link MuleCoreExtension} classes that are defined in the
 *  classpath using core-extensions.properties files.
 */
public class ClasspathMuleCoreExtensionDiscoverer implements MuleCoreExtensionDiscoverer
{

    public static final String SERVICE_PATH = "META-INF/services/org/mule/config/";
    public static final String CORE_EXTENSION_PROPERTIES = "core-extensions.properties";

    private static Log logger = LogFactory.getLog(ClasspathMuleCoreExtensionDiscoverer.class);

    @Override
    public List<MuleCoreExtension> discover() throws DefaultMuleException
    {
        List<MuleCoreExtension> result = new LinkedList<MuleCoreExtension>();

        Enumeration<?> e = ClassUtils.getResources(SERVICE_PATH + CORE_EXTENSION_PROPERTIES, getClass());
        List<Properties> extensions = new LinkedList<Properties>();

        // load ALL of the extension files first
        while (e.hasMoreElements())
        {
            try
            {
                URL url = (URL) e.nextElement();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Reading extension file: " + url.toString());
                }
                Properties p = new Properties();
                p.load(url.openStream());
                extensions.add(p);
            }
            catch (Exception ex)
            {
                throw new DefaultMuleException("Error loading Mule core extensions", ex);
            }
        }

        for (Properties extProps : extensions)
        {
            for (Map.Entry entry : extProps.entrySet())
            {
                String extName = (String) entry.getKey();
                String extClass = (String) entry.getValue();
                try
                {
                    MuleCoreExtension extension = (MuleCoreExtension) ClassUtils.instanciateClass(extClass);
                    result.add(extension);
                }
                catch (Exception ex)
                {
                    throw new DefaultMuleException("Error starting Mule core extension " + extName, ex);
                }
            }
        }

        return result;
    }
}
