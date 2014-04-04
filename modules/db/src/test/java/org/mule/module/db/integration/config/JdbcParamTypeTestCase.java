/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.module.db.domain.type.DbType;
import org.mule.module.db.domain.type.JdbcTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized;

/**
 *  Checks that every type defined in {@link JdbcTypes} can be used on a template parameter
 */
public class JdbcParamTypeTestCase extends AbstractParamTypeTestCase
{

    public JdbcParamTypeTestCase(String dbTypeName)
    {
        super(dbTypeName);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        Collection<DbType> list = JdbcTypes.list();

        List<Object[]> parameterSets = new ArrayList<Object[]>();

        for (DbType dbType : list)
        {
            parameterSets.add(new Object[] {dbType.getName()});
        }

        return parameterSets;
    }
}
