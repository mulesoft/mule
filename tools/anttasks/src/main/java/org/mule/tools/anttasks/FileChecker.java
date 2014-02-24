/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.anttasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;

import java.io.File;
import java.text.MessageFormat;

/**
* Check file parameters to Ant tasks
*/
public class FileChecker extends Task
{
    private Location location;

    /**
* Create a FileChecker
*/
    public FileChecker(Location location)
    {
        this.location = location;
    }

    /**
* Check that a specified file parameter is valid
*/
    public void checkFile(File file, String parameterName, boolean isDirectory, boolean needWrite) throws BuildException
    {
        if (file == null)
        {
            throw new BuildException(MessageFormat.format("No {0} specified", parameterName), getLocation());
        }
        if (!file.exists())
        {
            throw new BuildException(
                MessageFormat.format("{0} \"{1}\" does not exist", parameterName, file),
                getLocation());
        }
        if (!file.canRead())
        {
            throw new BuildException(
                MessageFormat.format("{0} \"{1}\" is not readable", parameterName, file),
                getLocation());
        }
        if (needWrite && !file.canWrite())
        {
            throw new BuildException(
                MessageFormat.format("{0} \"{1}\" is not writable", parameterName, file),
                getLocation());
        }
        if (isDirectory)
        {
            if (!file.isDirectory())
            {
                throw new BuildException(
                    MessageFormat.format("{0} \"{1}\" is not a directory", parameterName, file),
                    getLocation());
            }
        }
        else
        {
            if (file.isDirectory())
            {
                throw new BuildException(
                    MessageFormat.format("{0} \"{1}\" is a directory", parameterName, file),
                    getLocation());
            }
        }
    }
}

