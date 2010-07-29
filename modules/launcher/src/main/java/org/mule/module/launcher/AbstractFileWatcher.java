/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import java.io.File;

public abstract class AbstractFileWatcher implements Runnable
{

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
            onChange(file);
        }
    }

    protected abstract void onChange(File file);
}
