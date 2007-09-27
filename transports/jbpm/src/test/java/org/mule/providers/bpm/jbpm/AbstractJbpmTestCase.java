/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm;

import org.mule.providers.bpm.tests.AbstractBpmTestCase;
import org.mule.util.MuleDerbyTestUtils;

import java.io.InputStream;

public abstract class AbstractJbpmTestCase extends AbstractBpmTestCase
{
    private static boolean derbySetupDone = false;

    protected void suitePreSetUp() throws Exception
    {
        if (!derbySetupDone)
        {
            InputStream propertiesStream = this.getClass().getClassLoader().getResourceAsStream("derby.properties");
            String dbName = MuleDerbyTestUtils.loadDatabaseName(propertiesStream, "database.name");
            System.getProperties().put("hibernate.dbURL", "jdbc:derby:" + dbName + ";sql.enforce_strict_size=true");

            propertiesStream = this.getClass().getClassLoader().getResourceAsStream("derby.properties");
            MuleDerbyTestUtils.defaultDerbyCleanAndInit(propertiesStream, "database.name");
            derbySetupDone = true;
        }

        super.suitePreSetUp();
    }

}
