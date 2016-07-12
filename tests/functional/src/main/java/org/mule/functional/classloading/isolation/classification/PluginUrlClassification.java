/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import java.net.URL;
import java.util.List;

/**
 * Defines the result of the classification process for a plugin.
 * It contains a {@link List} of {@link URL}s that should have the plugin {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}
 * in order to run the test and a name for it.
 *
 * @since 4.0
 */
public class PluginUrlClassification
{
    private List<URL> urls;
    private String name;

    /**
     * Creates an instance of the classification.
     *
     * @param name a {@link String} representing the name of the plugin
     * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}
     */
    public PluginUrlClassification(String name, List<URL> urls)
    {
        this.name = name;
        this.urls = urls;
    }

    public List<URL> getUrls()
    {
        return urls;
    }

    public String getName()
    {
        return name;
    }
}
