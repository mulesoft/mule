/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.boot;

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
    }
}
