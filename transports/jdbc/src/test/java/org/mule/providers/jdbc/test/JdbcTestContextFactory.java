/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc.test;

import org.mule.tck.jndi.TestContextFactory;

import javax.naming.Context;
import javax.naming.NamingException;

public class JdbcTestContextFactory extends TestContextFactory
{
    protected void populateTestData(Context context) throws NamingException
    {
        super.populateTestData(context);
        context.bind("jdbc/testDS", new TestDataSource());
    }
}


