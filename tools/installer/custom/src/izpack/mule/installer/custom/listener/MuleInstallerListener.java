/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package com.izpack.mule.installer.custom.listener;

import com.izforge.izpack.Pack;
import com.izforge.izpack.event.SimpleInstallerListener;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.util.AbstractUIProgressHandler;

import java.io.File;
import java.util.List;

public class MuleInstallerListener extends SimpleInstallerListener
{
    /**
     * Default constructor
     */
    public MuleInstallerListener()
    {
        super();
    }

    public void beforePacks(AutomatedInstallData idata, Integer npacks, AbstractUIProgressHandler handler) throws Exception
    {
        List availablePacks = idata.availablePacks;
        for (int i = 0; i < availablePacks.size(); i++)
        {
            String packagePath = "/" + ((Pack)availablePacks.get(i)).name;
            File dir = new File(idata.getInstallPath() + packagePath);
            if (dir.exists())
            {
                deleteDir(dir);
            }
        }
    }

    public void afterPacks(AutomatedInstallData idata, AbstractUIProgressHandler handler) throws Exception
    {
        if (!idata.installSuccess)
        {
            List availablePacks = idata.availablePacks;
            for (int i = 0; i < availablePacks.size(); i++)
            {
                String packagePath = "/" + ((Pack)availablePacks.get(i)).name;
                File dir = new File(idata.getInstallPath() + packagePath);
                if (dir.exists())
                {
                    deleteDir(dir);
                }
            }
        }
    }

    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir)
    {
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }
}
