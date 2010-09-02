/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Clean the build path for a Mule application
 *
 * @execute lifecycle="mule-package" phase="package"
 * @goal clean
 * @requiresDependencyResolution runtime
 */
public class MuleCleanMojo extends AbstractMuleMojo {

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        final File app = new File(this.outputDirectory, this.finalName);
        if (app.exists()) {
            final boolean success = app.delete();
            if (success) {
                getLog().info("Deleted Mule App: "+ app);
            } else {
                getLog().info("Failed to delete Mule App: "+ app);
            }
        } else {
            getLog().info("Nothing to clean");
        }
    }

}