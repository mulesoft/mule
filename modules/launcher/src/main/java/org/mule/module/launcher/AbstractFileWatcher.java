/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFileWatcher implements Runnable
{
    protected Log logger = LogFactory.getLog(getClass());

    private long timeStamp;
    private Collection<File> files;

    public AbstractFileWatcher(File file)
    {
        this(Arrays.asList(file));
    }

    public AbstractFileWatcher(Collection<File> files)
    {
        this.files = files;
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public final void run()
    {
        long lastTimeStamp = timeStamp;
        File latestFile = null;

        for (File file : files)
        {
            long timestamp = file.lastModified();
            if (timestamp > lastTimeStamp)
            {
                lastTimeStamp = timeStamp;
                latestFile = file;
            }
        }

        if (latestFile != null)
        {
            this.timeStamp = lastTimeStamp;
            try
            {
                onChange(latestFile);
            }
            catch (Throwable t)
            {
                logger.error(String.format("Monitor for %s threw an exception", latestFile), t);
            }
        }
    }

    protected abstract void onChange(File file);
}
