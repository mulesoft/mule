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
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ContactDetails implements SQLData, Externalizable
{

    private String description;
    private String phoneNumber;
    private String email;
    private String sqlType = "CONTACT_DETAILS";

    @SuppressWarnings({"unused"})
    public ContactDetails()
    {
        // Used by the JDBC driver to create mapped instances
    }

    public ContactDetails(String description, String phoneNumber, String email)
    {
        this.description = description;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getDescription()
    {
        return description;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public String getEmail()
    {
        return email;
    }

    public String getSQLTypeName()
    {
        return sqlType;
    }

    public void readSQL(SQLInput stream, String type)
            throws SQLException
    {
        sqlType = type;
        description = stream.readString();
        phoneNumber = stream.readString();
        email = stream.readString();
    }

    public void writeSQL(SQLOutput stream)
            throws SQLException
    {
        stream.writeString(description);
        stream.writeString(phoneNumber);
        stream.writeString(email);
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

        ContactDetails contactDetails = (ContactDetails) o;

        if (!description.equals(contactDetails.description))
        {
            return false;
        }
        if (!phoneNumber.equals(contactDetails.phoneNumber))
        {
            return false;
        }
        return email.equals(contactDetails.email);

    }

    @Override
    public int hashCode()
    {
        int result = description.hashCode();
        result = 31 * result + phoneNumber.hashCode();
        result = 31 * result + email.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "ContactDetails{" +
               "email='" + email + '\'' +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", description='" + description + '\'' +
               '}';
    }

    public Object[] asObjectArray()
    {
        return new Object[] {description, phoneNumber, email};
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(description);
        out.writeObject(phoneNumber);
        out.writeObject(email);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        description = (String) in.readObject();
        phoneNumber = (String) in.readObject();
        email = (String) in.readObject();
    }
}
