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
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getContainerAppPluginsFolder;

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
    private List<ApplicationPluginDescriptor> containerApplicationPluginDescriptors;
    private final ApplicationPluginDescriptorFactory pluginDescriptorFactory;

    /**
     * @param pluginDescriptorFactory a {@link ApplicationPluginDescriptorFactory} for creating from the container applications plugins folder the list of {@link ApplicationPluginDescriptor}'s
     */
    public DefaultApplicationPluginRepository(ApplicationPluginDescriptorFactory pluginDescriptorFactory)
    {
        this.pluginDescriptorFactory = pluginDescriptorFactory;
    }

    @Override
    public synchronized List<ApplicationPluginDescriptor> getContainerApplicationPluginDescriptors() throws IOException
    {
        if (containerApplicationPluginDescriptors == null)
        {
            collectContainerApplicationPluginDescriptors();
        }
        return containerApplicationPluginDescriptors;
    }

    private void collectContainerApplicationPluginDescriptors() throws IOException
    {
        File[] containerPlugins = getContainerAppPluginsFolder().listFiles();
        if (containerPlugins != null)
        {
            unzipPluginsIfNeeded();
            containerApplicationPluginDescriptors = unmodifiableList(createApplicationPluginDescriptors());
        }
        else
        {
            containerApplicationPluginDescriptors = EMPTY_LIST;
        }
    }

    /**
     * Iterates the list of zip files in container application plugin folder, unzip them and once the plugin is expanded
     * it deletes the zip from the container app plugins folder.
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
