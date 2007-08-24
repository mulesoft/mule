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

import org.mule.tools.visualizer.config.GraphEnvironment;

import java.io.File;

public class DocIndexerPostGrapher extends AbstractIndexer
{

    public static final String DEFAULT_MULE_DOC_INDEXER_TEMPLATE = "template/doc-index.vm";

    private String template;

    public DocIndexerPostGrapher(GraphEnvironment env) throws Exception
    {
        super(env);
        template = env.getProperties().getProperty("muleDocIndexerTemplate");
        if (template == null)
        {
            template = DEFAULT_MULE_DOC_INDEXER_TEMPLATE;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tools.visualizer.PostGrapher#postGrapher(org.mule.tools.visualizer.GraphConfig)
     */
    public void postGrapher(GraphEnvironment env)
    {
        File[] htmlFiles = getFiles(env.getConfig(), ".html");
        String targetFile = env.getConfig().applyOutputDirectory(
                env.getProperty("muleDocIndexerOutputName", "index.html"));
        doRendering(env, htmlFiles, template, targetFile);

    }

    public String getStatusTitle()
    {
        return "Generating Index page";
    }

}
