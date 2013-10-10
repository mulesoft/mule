/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.config;

import org.mule.config.AnnotationsConfigurationBuilder;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.scan.ClasspathScanner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Provides implementation support for configuration builders that configure Mule by scanning annotations on the
 * classpath.
 *
 * One of more scan packages can be used to locate classes with annotations. The packages to be scanned cn be configured
 * in two ways:
 *
 * 1) Pass one or more comma-separated packages into the constructor of this builder
 * 2) if no packages are set via the constructor or the {@link #DEFAULT_BASE_PACKAGE} value is used, the classpath will be
 * scanned for <code>META-INF/ibeans.properties</code>. Zero or more of these will be loaded and the package names defined in
 * either the 'ibeans.scan.packages' or 'annotations.scan.packages' will be scanned.  This allows users to configure
 * specific packages to scan in their application.
 */
public abstract class AbstractAnnotationConfigurationBuilder extends AnnotationsConfigurationBuilder
{
    public static final String IBEANS_PROPERTIES = "META-INF/ibeans-app.properties";

    public static final String[] DEFAULT_BASE_PACKAGE = new String[]{""};


    protected ClassLoader classLoader;
    protected String[] basepackages;

    public AbstractAnnotationConfigurationBuilder()
    {
        this(DEFAULT_BASE_PACKAGE);
    }

    public AbstractAnnotationConfigurationBuilder(String... basepackages)
    {
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.basepackages = basepackages;
    }

    public AbstractAnnotationConfigurationBuilder(ClassLoader classLoader)
    {
        this(classLoader, DEFAULT_BASE_PACKAGE);
    }

    public AbstractAnnotationConfigurationBuilder(ClassLoader classLoader, String... basepackages)
    {
        this.classLoader = classLoader;
        this.basepackages = basepackages;
    }

    protected ClasspathScanner createClasspathScanner() throws IOException
    {
        if (Arrays.equals(DEFAULT_BASE_PACKAGE, basepackages))
        {
            basepackages = findPackages();
        }

        String[] paths = convertPackagesToPaths(basepackages);
        if(logger.isInfoEnabled())
        {
            logger.info("Scanning for annotations using the following paths: " + StringMessageUtils.toString(paths));
        }
        return new ClasspathScanner(classLoader, paths);
    }

    protected abstract String getScanPackagesProperty();

    protected String[] convertPackagesToPaths(String[] packages)
    {
        String[] paths = new String[packages.length];
        for (int i = 0; i < packages.length; i++)
        {
            paths[i] = packages[i].replaceAll("[.]", "/");
        }
        return paths;
    }

    protected String[] findPackages() throws IOException
    {
        List<String> paths = new ArrayList<String>();
        Properties p = new Properties();
        Enumeration e = ClassUtils.getResources(IBEANS_PROPERTIES, getClass());
        boolean scanAll = false;
        while (e.hasMoreElements())
        {
            URL url = (URL) e.nextElement();

            if(logger.isInfoEnabled()) logger.info("reading packages from: " + url);
            p.load(url.openStream());
            String path = p.getProperty(getScanPackagesProperty());
            if (path != null)
            {
                for (StringTokenizer tokenizer = new StringTokenizer(path, ","); tokenizer.hasMoreTokens();)
                {
                    String s = tokenizer.nextToken();
                    if("*".equals(s)) {
                        scanAll=true;
                        break;
                    }
                    paths.add(s.trim());
                }
            }
        }

        if (paths.size() == 0 || scanAll)
        {
            return DEFAULT_BASE_PACKAGE;
        }
        return paths.toArray(new String[]{});
    }
}
