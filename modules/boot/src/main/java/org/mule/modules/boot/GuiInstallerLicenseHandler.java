/*
 * $Id $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

public class GuiInstallerLicenseHandler
{
    /**
     * The main method which is called by the GUI. It creates the LicenseHandler
     * object and then seeks to save the license by calling the method
     * saveLicenseAck.
     * 
     */
    public static void main(String args[]) throws Exception
    {
        System.setProperty("mule.home", args[0]);
        LicenseHandler.saveLicenseInfo(new LicenseHandler.LicenseInfo());
    }
}
