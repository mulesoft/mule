/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.postgraphers;

import org.mule.tools.visualizer.components.PostGrapher;
import org.mule.tools.visualizer.config.GraphEnvironment;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MediaCopierPostGrapher implements PostGrapher
{

    public static final String MEDIA = "media";

    public String getStatusTitle()
    {
        return "Copy Media files (logo, css,...)";
    }

    public void postGrapher(GraphEnvironment env)
    {

        try
        {
            boolean copied = false;
            String path = org.mule.util.FileUtils.getResourcePath(MEDIA, getClass());
            if (null != path)
            {
                File media = new File(path);
                if (media.exists() && media.isDirectory())
                {
                    FileUtils.copyDirectory(media, env.getConfig().getOutputDirectory());
                    copied = true;
                }
            }
            if (!copied)
            {
                env.log("Could not find directory " + MEDIA);
            }
        }
        catch (IOException e)
        {
            env.logError(e.getMessage(), e);
        }
    }

}
