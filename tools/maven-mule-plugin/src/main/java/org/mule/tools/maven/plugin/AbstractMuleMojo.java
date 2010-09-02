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

import org.apache.maven.plugin.AbstractMojo;

/**
 * Base Mule Application Mojo
 */
public abstract class AbstractMuleMojo extends AbstractMojo
{
    /**
     * Directory containing the generated Mule App.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;
    /**
     * Name of the generated Mule App.
     *
     * @parameter alias="appName" expression="${appName}" default-value="${project.build.finalName}"
     * @required
     */
    protected String finalName;
}