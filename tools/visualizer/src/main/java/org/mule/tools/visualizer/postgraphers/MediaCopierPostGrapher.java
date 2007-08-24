/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.postgraphers;

import org.mule.tools.visualizer.components.PostGrapher;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.util.FileUtils;

import java.io.IOException;


public class MediaCopierPostGrapher implements PostGrapher
{

    public static final String MEDIA = "media";

    public String getStatusTitle()
    {
        return "Copy Media files (logo, css,...)";
    }

    /**
     * Extract media dir from mule-tools-visualizer.jar and copy it to output dir
     *
     * @param env enviropment and config variables
     */
    public void postGrapher(GraphEnvironment env)
    {
        try
        {
            FileUtils.extractResources(MEDIA, getClass(), env.getConfig().getOutputDirectory(), false);
        }
        catch (IOException e)
        {
            env.logError(e.getMessage(), e);
        }
    }

}
