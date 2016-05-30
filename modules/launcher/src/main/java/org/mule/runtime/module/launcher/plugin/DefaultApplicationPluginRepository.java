/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import static java.io.File.separator;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getContainerAppPluginsFolder;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Default implementation of an {@link ApplicationPluginRepository} that holds in memory the list
 * of artifact plugin descriptors bundled with the container.
 *
 * @since 4.0
 */
public class DefaultApplicationPluginRepository implements ApplicationPluginRepository
{
    private volatile List<ApplicationPluginDescriptor> containerApplicationPluginDescriptors;

    private final ApplicationPluginDescriptorFactory pluginDescriptorFactory;

    /**
     * @param pluginDescriptorFactory a {@link ApplicationPluginDescriptorFactory} for creating from the container applications plugins folder the list of {@link ApplicationPluginDescriptor}'s
     */
    public DefaultApplicationPluginRepository(ApplicationPluginDescriptorFactory pluginDescriptorFactory)
    {
        checkArgument(pluginDescriptorFactory != null, "Application plugin descriptor factory cannot be null");
        this.pluginDescriptorFactory = pluginDescriptorFactory;
    }

    @Override
    public List<ApplicationPluginDescriptor> getContainerApplicationPluginDescriptors()
    {
        if (containerApplicationPluginDescriptors == null)
        {
            synchronized (this)
            {
                if (containerApplicationPluginDescriptors == null)
                {
                    try
                    {
                        containerApplicationPluginDescriptors = unmodifiableList(collectContainerApplicationPluginDescriptors());
                    }
                    catch (IOException e)
                    {
                        throw new ArtifactDescriptorCreateException("Cannot load application plugin descriptors from container", e);
                    }
                }
            }
        }
        return containerApplicationPluginDescriptors;
    }

    /**
     * @return collects and initializes a {@link List} of {@link ApplicationPluginDescriptor} by loading the container application plugins
     * @throws IOException
     */
    private List<ApplicationPluginDescriptor> collectContainerApplicationPluginDescriptors() throws IOException
    {
        File[] containerPlugins = getContainerAppPluginsFolder().listFiles();
        if (containerPlugins != null)
        {
            unzipPluginsIfNeeded();
            return createApplicationPluginDescriptors();
        }
        else
        {
            return EMPTY_LIST;
        }
    }

    /**
     * Iterates the list of zip files in container application plugin folder, unzip them and once the plugin is expanded
     * it deletes the zip from the container app plugins folder.
     *
     * @throws IOException
     */
    private void unzipPluginsIfNeeded() throws IOException
    {
        for (File pluginZipFile : getContainerAppPluginsFolder().listFiles((FileFilter) new SuffixFileFilter(".zip", INSENSITIVE)))
        {
            String pluginName = removeExtension(pluginZipFile.getName());

            final File pluginFolderExpanded = new File(getContainerAppPluginsFolder(),
                                                       separator + pluginName);
            unzip(pluginZipFile, pluginFolderExpanded);

            forceDelete(pluginZipFile);
        }
    }

    /**
     * For each plugin expanded in container application plugins folder it creates an {@link ApplicationPluginDescriptor} for it and
     * adds the descriptor the given list.
     *
     * @return a non null {@link List} of {@link ApplicationPluginDescriptor}
     */
    private List<ApplicationPluginDescriptor> createApplicationPluginDescriptors()
    {
        List<ApplicationPluginDescriptor> pluginDescriptors = new LinkedList<>();

        for (File pluginExpandedFolder : getContainerAppPluginsFolder().listFiles((FileFilter) DirectoryFileFilter.DIRECTORY))
        {
            final ApplicationPluginDescriptor appPluginDescriptor = pluginDescriptorFactory.create(pluginExpandedFolder);
            pluginDescriptors.add(appPluginDescriptor);
        }

        return pluginDescriptors;
    }
}
