/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

import org.mule.extensions.annotations.Parameter;

import java.util.Calendar;
import java.util.Date;

public class LifetimeInfo
{

    @Parameter
    private Date dateOfBirth;

    @Parameter
    private Calendar dateOfDeath;

    public Date getDateOfBirth()
    {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public Calendar getDateOfDeath()
    {
        return dateOfDeath;
    }

    public void setDateOfDeath(Calendar dateOfDeath)
    {
        this.dateOfDeath = dateOfDeath;
    }
}
