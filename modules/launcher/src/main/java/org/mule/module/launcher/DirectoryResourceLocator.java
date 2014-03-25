/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class DirectoryResourceLocator implements LocalResourceLocator
{

    private String[] directories;

    public DirectoryResourceLocator(String... directories)
    {
        this.directories = directories;
    }

    public URL findLocalResource(String resourceName)
    {
        if (directories != null && resourceName != null)
        {
            for (String directory : directories)
            {
                File resourceFile = new File(directory, resourceName);
                if (resourceFile.exists())
                {
                    try
                    {
                        return resourceFile.toURI().toURL();
                    }
                    catch (MalformedURLException e)
                    {
                        throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("Can not load resource with name %s.", resourceName)), e);
                    }
                }
            }
        }
        return null;
    }
}