/*
 * $Id: $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tools.config.graph.postgraphers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphEnvironment;

public class MediaCopierPostGrapher implements PostGrapher
{

    public String getStatusTitle()
    {
        return "Copy Media files (logo, css,...)";
    }

    public void postGrapher(GraphEnvironment env)
    {

        try {
            FileUtils.copyDirectory(new File("./src/resources/media/"), env.getConfig()
                            .getOutputDirectory());
        }
        catch (IOException e) {
            env.logError(e.getMessage(), e);

        }
    }

}
