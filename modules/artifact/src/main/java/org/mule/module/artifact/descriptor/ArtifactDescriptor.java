/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.descriptor;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class ArtifactDescriptor
{

    private String name;
    private File rootFolder;
    private Set<String> loaderOverrides = Collections.emptySet();
    private Set<String> exportedPrefixNames = Collections.emptySet();
    private Set<String> blockedPrefixNames = Collections.emptySet();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public File getRootFolder()
    {
        return rootFolder;
    }

    public void setRootFolder(File rootFolder)
    {
        if (rootFolder == null)
        {
            throw new IllegalArgumentException("Root folder cannot be null");
        }

        this.rootFolder = rootFolder;
    }

    public void setLoaderOverride(Set<String> loaderOverrides)
    {
        if (loaderOverrides == null)
        {
            throw new IllegalArgumentException("Loader overrides cannot be null");
        }

        this.loaderOverrides = Collections.unmodifiableSet(loaderOverrides);
    }

    public Set<String> getLoaderOverrides()
    {
        return loaderOverrides;
    }

    public void setExportedPrefixNames(Set<String> exported)
    {
        this.exportedPrefixNames = Collections.unmodifiableSet(exported);
    }

    /**
     * @return an immutable set of exported class prefix names
     */
    public Set<String> getExportedPrefixNames()
    {
        return exportedPrefixNames;
    }

    public void setBlockedPrefixNames(Set<String> blocked)
    {
        this.blockedPrefixNames = Collections.unmodifiableSet(blocked);
    }

    /**
     * @return an immutable set of blocked class prefix names
     */
    public Set<String> getBlockedPrefixNames()
    {
        return blockedPrefixNames;
    }
}
