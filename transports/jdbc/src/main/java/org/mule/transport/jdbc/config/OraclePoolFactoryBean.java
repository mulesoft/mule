/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

public class OraclePoolFactoryBean extends AbstractPoolFactoryBean
{
    private static final String DEFAULT_JDBC_URL = "jdbc:oracle:thin:@localhost:1521:orcl";
    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";

    public OraclePoolFactoryBean()
    {
        super();
        url = DEFAULT_JDBC_URL;
        driverClassName = DRIVER_CLASS_NAME;
    }
}
