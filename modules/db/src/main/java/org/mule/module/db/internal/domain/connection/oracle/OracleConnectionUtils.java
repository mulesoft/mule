/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.connection.oracle;

/**
 * Utils for the oracle db connection
 *
 */
public class OracleConnectionUtils
{
    public static String getOwnerFrom(String typeName)
    {
        if (typeName.indexOf(".") == -1)
        {
            return null;
        }
        else
        {
            return typeName.substring(0, typeName.indexOf("."));
        }
    }

    public static String getTypeSimpleName(String typeName)
    {
        if (typeName.indexOf(".") == -1)
        {
            return typeName;
        }
        else
        {
            return typeName.substring(typeName.indexOf(".")+1);
        }
    }
}
