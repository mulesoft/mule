/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFileWatcher implements Runnable
{

    protected Log logger = LogFactory.getLog(getClass());

    private Collection<File> files;
    private Map<File, Long> timestamps = new HashMap<File, Long>();

    public AbstractFileWatcher(File file)
    {
        this(Arrays.asList(file));
    }

    public AbstractFileWatcher(Collection<File> files)
    {
        this.files = files;

        for (File file : files)
        {
            timestamps.put(file, file.lastModified());
        }
    }

    @Override
    public final void run()
    {
        File latestFile = null;

        for (File file : files)
        {
            long originalTimestamp = timestamps.get(file);
            long currentTimestamp = file.lastModified();

            if (originalTimestamp != currentTimestamp)
            {
                timestamps.put(file, currentTimestamp);
                latestFile = file;
            }
        }

        if (latestFile != null)
        {
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
