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

import org.mule.tools.config.graph.config.GraphEnvironment;

public class DocIndexerPostGrapher extends AbstractIndexer
{

    public static final String DEFAULT_MULE_DOC_INDEXER_TEMPLATE = "./src/resources/template/doc-index.vm";

    private String template;

    public DocIndexerPostGrapher(GraphEnvironment env) throws Exception
    {
        super(env);
        template = env.getProperties().getProperty("muleDocIndexerTemplate");
        if (template == null) {
            template = DEFAULT_MULE_DOC_INDEXER_TEMPLATE;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tools.config.graph.PostGrapher#postGrapher(org.mule.tools.config.graph.GraphConfig)
     */
    public void postGrapher(GraphEnvironment env)
    {
        File[] htmlFiles = getFiles(env.getConfig(), ".html");
        String targetFile = env.getConfig().applyWorkingDirectory(
                        env.getProperty("muleDocIndexerOutputName", "index.html"));
        doRendering(env, htmlFiles, template, targetFile);

    }

    public String getStatusTitle()
    {
        return "Generating Index page";
    }

}
