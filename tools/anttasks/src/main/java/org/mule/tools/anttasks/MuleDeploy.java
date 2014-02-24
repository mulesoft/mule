/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.anttasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
* Ant task to deploy a Mule application to the MULE_HOME/apps directory.
*
* XML format
*
* <taskdef name="muleDeploy" classname="org.mule.tools.anttasks.MuleDeploy"/>
*
* <muleDeploy applicationFile="file" muleHome="dir"/>
*
* If muleHome isn't specified, the value of the Ant property dir.mule.home is used.
*/
public class MuleDeploy extends Task
{
    /** The Ant property containing the default Mule home directory */
    private static final String MULE_HOME_PROPERTY = "dir.mule.home";

    /** The application file to install */
    private File applicationFile;

    /** The home directory of the mule installation to install the application into */
    private File muleHome;

    /**
* Specify the Mule application file to install
*/
    public void setApplicationFile(File applicationFile)
    {
        this.applicationFile = applicationFile;
    }

    /**
* install the application file
*/
    @Override
    public void execute() throws BuildException
    {
        FileChecker checker = new FileChecker(getLocation());
        checker.checkFile(applicationFile, "application file", false, false);
        File home = getMuleHome();
        checker.checkFile(home, "mule home directory", true, false);
        File appsDir = new File(home, "apps");
        checker.checkFile(appsDir, "mule apps directory", true, true);
        try
        {
            File destFile = new File(appsDir, applicationFile.getName());
            FileUtils.getFileUtils().copyFile(applicationFile, destFile, null, true);
        }
        catch (IOException ex)
        {
            throw new BuildException(
                MessageFormat.format("Problem copying Mule application file {0} to {1}", applicationFile, appsDir),
                ex, getLocation());
        }
    }

    /**
* @return the Mule home directory
*/
    public File getMuleHome()
    {
        if (muleHome != null)
        {
            return muleHome;
        }
        String home = getProject().getProperty(MULE_HOME_PROPERTY);
        return home == null ? null : new File(home);
    }

    /**
* Specify the Mule home directory
*/
    public void setMuleHome(File muleHome)
    {
        this.muleHome = muleHome;
    }
}

