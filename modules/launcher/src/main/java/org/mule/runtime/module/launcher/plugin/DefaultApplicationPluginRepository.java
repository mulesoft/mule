/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import static java.io.File.separator;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getContainerAppPluginsFolder;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Default implemmentation of an {@link ApplicationPluginRepository} that holds in memory the list
 * of artifact plugin descriptors bundled with the container.
 *
 * @since 4.0
 */
public class DefaultApplicationPluginRepository implements ApplicationPluginRepository
{
    private List<ApplicationPluginDescriptor> containerApplicationPluginDescriptors;
    private final ApplicationPluginDescriptorFactory pluginDescriptorFactory;

    public DefaultApplicationPluginRepository(ApplicationPluginDescriptorFactory pluginDescriptorFactory)
    {
        this.pluginDescriptorFactory = pluginDescriptorFactory;
    }

    @Override
    public List<ApplicationPluginDescriptor> getContainerApplicationPluginDescriptors() throws IOException
    {
        if(containerApplicationPluginDescriptors == null)
        {
            collectContainerApplicationPluginDescriptors();
        }
        return containerApplicationPluginDescriptors;
    }

    private void collectContainerApplicationPluginDescriptors() throws IOException
    {
        List<ApplicationPluginDescriptor> pluginDescriptors = new LinkedList<>();

        File[] containerPlugins = getContainerAppPluginsFolder().listFiles();
        if (containerPlugins != null)
        {
            // First iteration over zip plugins to expand and delete
            for (File pluginZipFile : getContainerAppPluginsFolder().listFiles((FileFilter) new SuffixFileFilter(".zip", IOCase.INSENSITIVE)))
            {
                String pluginName = FilenameUtils.removeExtension(pluginZipFile.getName());

                // must unpack as there's no straightforward way for a ClassLoader to use a zip within another jar/zip
                final File pluginFolderExpanded = new File(getContainerAppPluginsFolder(),
                                             separator + pluginName);
                FileUtils.unzip(pluginZipFile, pluginFolderExpanded);

                // now we don't need to have the zip file anymore so deleting it
                FileUtils.forceDelete(pluginZipFile);
            }

            // Load core application plugins
            for (File pluginExpandedFolder : getContainerAppPluginsFolder().listFiles((FileFilter) DirectoryFileFilter.DIRECTORY))
            {
                final ApplicationPluginDescriptor appPluginDescriptor = pluginDescriptorFactory.create(pluginExpandedFolder);
                pluginDescriptors.add(appPluginDescriptor);
            }
        }

        containerApplicationPluginDescriptors = Collections.unmodifiableList(pluginDescriptors);
    }
}
