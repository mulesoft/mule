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
package org.mule.tools.mc4j;

import org.mc4j.console.connection.install.J2SE5ConnectionTypeDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConnectionTypeDescriptor extends J2SE5ConnectionTypeDescriptor
{

    private String version;

    public MuleConnectionTypeDescriptor() throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("mule-version.properties");
        if(is==null) {
            throw new FileNotFoundException("Failed to find mule-version.properties on the classpath");
        }
        Properties p = new Properties();
        p.load(is);
        version = p.getProperty("mule.version");
    }

    public boolean isMEJBCompliant()
    {
        return false;
    }

    public String getDisplayName()
    {
        return "Mule";
    }

    public String[] getConnectionClasspathEntries()
    {
        return (new String[] {"mule-" + version + ".jar"});
    }

    public String getRecongnitionPath()
    {
        return "lib/mule-" + version + ".jar";
    }

    public String getConnectionType()
    {
        return "Mule";
    }
}
