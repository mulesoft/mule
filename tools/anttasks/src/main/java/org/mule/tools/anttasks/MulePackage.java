/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.anttasks;

import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;

/**
* Ant task to package a mule application
*
* XML format:
*
* <taskdef name="mulePackage" classname="org.mule.tools.anttasks.MulePackage"/>
*
* <mulePackage applicationFile="file">
* <config (fileSet)/>
* <classes (fileSet)/>
* <lib (fileSet)/>
* </mulePackage>
*/
public class MulePackage extends Zip
{
    /**
* Specify the Mule application file to install
*/
    public void setApplicationFile(File applicationFile)
    {
        setDestFile(applicationFile);
    }

    /**
* add config files at top level
* @param fs the zip file set to add
*/
    public void addConfig(ZipFileSet fs)
    {
        super.addFileset(fs);
    }

    /**
* add files under lib
* @param fs the zip file set to add
*/
    public void addLib(ZipFileSet fs)
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("lib/");
        super.addFileset(fs);
    }

    /**
* add files under classes
* @param fs the zip file set to add
*/
    public void addClasses(ZipFileSet fs)
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("classes/");
        super.addFileset(fs);
    }

}
