/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classpath;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses java system properties to get the classpath urls.
 *
 * @since 4.0
 */
public class ClassPathUrlProvider
{

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * @return Gets the urls from the {@code java.class.path} and {@code sun.boot.class.path} system properties
     */
    public List<URL> getURLs()
    {
        final Set<URL> urls = new LinkedHashSet<>();
        addUrlsFromSystemProperty(urls, "java.class.path");
        addUrlsFromSystemProperty(urls, "sun.boot.class.path");

        if (logger.isDebugEnabled())
        {
            StringBuilder builder = new StringBuilder("ClassPath:");
            urls.stream().forEach(url -> builder.append(File.pathSeparator).append(url));
            logger.debug(builder.toString());
        }

        return Lists.newArrayList(urls);
    }

    protected void addUrlsFromSystemProperty(final Collection<URL> urls, final String propertyName)
    {
        for (String file : System.getProperty(propertyName).split(":"))
        {
            try
            {
                urls.add(new File(file).toURI().toURL());
            }
            catch (MalformedURLException e)
            {
                throw new IllegalArgumentException("Cannot create a URL from file path: " + file, e);
            }
        }
    }

}
