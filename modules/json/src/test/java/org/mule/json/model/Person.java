/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.json.model;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

/**
 * A Person object
 */
@JsonSerialize
public class Person
{
    private String name;
    private String dob;

    private List<EmailAddress> emailAddresses;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDob()
    {
        return dob;
    }

    public void setDob(String dob)
    {
        this.dob = dob;
    }

    public List<EmailAddress> getEmailAddresses()
    {
        return emailAddresses;
    }

    public void setEmailAddresses(List<EmailAddress> emailAddresses)
    {
        this.emailAddresses = emailAddresses;
    }
}
