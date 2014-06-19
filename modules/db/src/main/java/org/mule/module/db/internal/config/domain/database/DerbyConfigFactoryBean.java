/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

public class DerbyConfigFactoryBean extends DbConfigFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";

    public DerbyConfigFactoryBean()
    {
        super();
        setDriverClassName(DRIVER_CLASS_NAME);
    }
}
