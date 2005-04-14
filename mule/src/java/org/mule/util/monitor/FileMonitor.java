/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util.monitor;

import org.mule.util.monitor.FileMonitor.FileMonitorNotifier;

import java.beans.ExceptionListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Class for monitoring changes in disk files.
 * Usage:
 *
 *    1. Implement the FileListener interface.
 *    2. Create a FileMonitor instance.
 *    3. Add the file(s)/directory(ies) to listen for.
 *
 * fileChanged() will be called when a monitored file is created,
 * deleted or its modified time changes.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public class FileMonitor
{
    private Timer timer;
    private Map files;
    private List listeners;
    private long pollingInterval;


    /**
     * Create a file monitor instance with specified polling interval.
     *
     * @param pollingInterval Polling interval in milli seconds.
     */
    public FileMonitor(long pollingInterval)
    {
        files = new HashMap();
        listeners = new ArrayList();
        timer = new Timer(true);
        this.pollingInterval = pollingInterval;
    }


    /**
     * Stop the file monitor polling.
     */
    public void stop()
    {
        timer.cancel();
    }

    public void start() {
        timer.schedule(new FileMonitorNotifier(), 0, pollingInterval);
    }


    /**
     * Add file to listen for. File may be any java.io.File (including a
     * directory) and may well be a non-existing file in the case where the
     * creating of the file is to be trepped.
     * <p/>
     * More than one file can be listened for. When the specified file is
     * created, modified or deleted, listeners are notified.
     *
     * @param file File to listen for.
     */
    public void addFile(File file)
    {
        if (!files.containsKey(file))
        {
            long modifiedTime = file.exists() ? file.lastModified() : -1;
            files.put(file, new Long(modifiedTime));
        }
    }


    /**
     * Remove specified file for listening.
     *
     * @param file File to remove.
     */
    public void removeFile(File file)
    {
        files.remove(file);
    }


    /**
     * Add listener to this file monitor.
     *
     * @param fileListener Listener to add.
     */
    public void addListener(FileListener fileListener)
    {
        // Don't add if its already there
        for (Iterator i = listeners.iterator(); i.hasNext();)
        {
            WeakReference reference = (WeakReference) i.next();
            FileListener listener = (FileListener) reference.get();
            if (listener == fileListener)
                return;
        }

        // Use WeakReference to avoid memory leak if this becomes the
        // sole reference to the object.
        listeners.add(new WeakReference(fileListener));
    }


    /**
     * Remove listener from this file monitor.
     *
     * @param fileListener Listener to remove.
     */
    public void removeListener(FileMonitor fileListener)
    {
        for (Iterator i = listeners.iterator(); i.hasNext();)
        {
            WeakReference reference = (WeakReference) i.next();
            FileMonitor listener = (FileMonitor) reference.get();
            if (listener == fileListener)
            {
                i.remove();
                break;
            }
        }
    }


    /**
     * This is the timer thread which is executed every n milliseconds
     * according to the setting of the file monitor.
     */
    public class FileMonitorNotifier extends TimerTask
    {
        private ExceptionListener exceptionListener;

        public FileMonitorNotifier()
        {
        }

        public FileMonitorNotifier(ExceptionListener exceptionListener)
        {
            this.exceptionListener = exceptionListener;
        }

        public void run()
        {
            // Loop over the registered files and see which have changed.
            // Use a copy of the list in case listener wants to alter the
            // list within its fileChanged method.
            Collection fileKeys = new ArrayList(files.keySet());

            for (Iterator i = fileKeys.iterator(); i.hasNext();)
            {
                File file = (File) i.next();
                long lastModifiedTime = ((Long) files.get(file)).longValue();
                long newModifiedTime = file.exists() ? file.lastModified() : -1;

                // Chek if file has changed
                if (newModifiedTime != lastModifiedTime)
                {
                    // Register new modified time
                    files.put(file, new Long(newModifiedTime));

                    // Notify listeners
                    for (Iterator j = listeners.iterator(); j.hasNext();)
                    {
                        WeakReference reference = (WeakReference) j.next();
                        FileListener listener = (FileListener) reference.get();

                        // Remove from list if the back-end object has been GC'd
                        if (listener == null) {
                            j.remove();
                        } else {
                            try
                            {
                                listener.fileChanged(file);
                            } catch (IOException e)
                            {
                                if(exceptionListener!=null) {
                                    exceptionListener.exceptionThrown(e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
