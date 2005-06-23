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
package org.mule.tck;

import java.io.File;

import junit.framework.TestCase;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.util.StringMessageHelper;
import org.mule.util.Utility;

/**
 * <code>NamedTestCase</code> provides readable testcase names.
 * 
 * @author Joe Walnes
 * @version $Revision$
 */

public abstract class NamedTestCase extends TestCase
{
    protected NamedTestCase()
    {
        // when testing, do not set up server connections
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "true");
    }

    public String getName()
    {
        return super.getName().substring(4).replaceAll("([A-Z])", " $1").toLowerCase();
    }

    /**
     * Print the name of this test to standard output
     */
    protected void setUp() throws Exception
    {
        System.out.println(StringMessageHelper.getBoilerPlate("Testing: " + toString(), '=', 80));
        MuleManager.getConfiguration().getDefaultThreadingProfile().setDoThreading(false);
    }

    protected void tearDown() throws Exception
    {
        File f = new File(".mule");
        try {
            if (f.exists()) {
                Utility.deleteTree(f);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
