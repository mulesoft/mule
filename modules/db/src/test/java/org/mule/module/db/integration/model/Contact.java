/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

public class Contact
{
    public static final Contact CONTACT1 = new Contact("Contact1", new ContactDetails[] {new ContactDetails("home", "1-111-111", "1@1111.com")});
    public static final Contact CONTACT2 = new Contact("Contact2", new ContactDetails[] {new ContactDetails("work", "2-222-222", "2@2222.com")});

    private final String name;
    private final ContactDetails[] details;

    public Contact(String name, ContactDetails[] details)
    {
        this.name = name;
        this.details = details;
    }

    public String getName()
    {
        return name;
    }

    public ContactDetails[] getDetails()
    {
        return details;
    }

    public Object[] getDetailsAsObjectArray()
    {
        final Object[] result = new Object[details.length];
        for (int i = 0; i < details.length; i++)
        {
            result[i] = details[i].asObjectArray();
        }

        return result;
    }
}
