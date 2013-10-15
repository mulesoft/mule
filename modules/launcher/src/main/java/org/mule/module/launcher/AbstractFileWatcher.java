/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFileWatcher implements Runnable
{

    protected Log logger = LogFactory.getLog(getClass());

    private long timeStamp;
    private File file;

    public AbstractFileWatcher(File file)
    {
        this.file = file;
        this.timeStamp = file.lastModified();
    }

    public final void run()
    {
        long timeStamp = file.lastModified();

        if (this.timeStamp != timeStamp)
        {
            this.timeStamp = timeStamp;
            try
            {
                onChange(file);
            }
            catch (Throwable t)
            {
                logger.error(String.format("Monitor for %s threw an exception", file), t);
            }
        }
    }

    protected abstract void onChange(File file);
}
