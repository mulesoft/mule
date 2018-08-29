/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class BlobAndClobType implements SQLData, Externalizable
{

    private Blob remarkB;
    private Clob remarkC;

    private String sqlType = "BLOB_AND_CLOB_TYPE";

    @SuppressWarnings({"unused"})
    public BlobAndClobType()
    {
        // Used by the JDBC driver to create mapped instances
    }

    public BlobAndClobType(Blob remarkB, Clob remarkC)
    {
        this.remarkB = remarkB;
        this.remarkC = remarkC;
    }

    public Blob getRemarkB()
    {
        return remarkB;
    }

    public Clob getRemarkC()
    {
        return remarkC;
    }

    public String getSQLTypeName()
    {
        return sqlType;
    }

    public void readSQL(SQLInput stream, String type)
            throws SQLException
    {
        sqlType = type;
        remarkB = stream.readBlob();
        remarkC = stream.readClob();
    }

    public void writeSQL(SQLOutput stream)
            throws SQLException
    {
        stream.writeBlob(remarkB);
        stream.writeClob(remarkC);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        BlobAndClobType blobAndClob = (BlobAndClobType) o;

        if (!remarkB.equals(blobAndClob.remarkB))
        {
            return false;
        }
        if (!remarkC.equals(blobAndClob.remarkC))
        {
            return false;
        }
        
        return true;

    }

    @Override
    public int hashCode()
    {
        int result = remarkB.hashCode();
        result = 31 * result + remarkB.hashCode();
        return result;
    }


    public Object[] asObjectArray()
    {
        return new Object[] {remarkB, remarkC};
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(remarkB);
        out.writeObject(remarkC);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        remarkB = (Blob) in.readObject();
        remarkC = (Clob) in.readObject();
    }
}
