/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import java.io.File;
import java.net.URL;

public class HierarchyResourceLocator extends DirectoryResourceLocator
{

    private final ResourceLocator parent;

    public HierarchyResourceLocator(File directory, ResourceLocator parent)
    {
        super(directory);
        this.parent = parent;
    }

    @Override
    public URL locateResource(String name)
    {
        URL url = super.locateResource(name);
        if (url == null)
        {
            url = parent.locateResource(name);
        }
        return url;
    }
}
