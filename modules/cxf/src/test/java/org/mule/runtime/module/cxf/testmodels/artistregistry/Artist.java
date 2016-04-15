/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.testmodels.artistregistry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="artist", propOrder = {
        "artType",
        "famousWorks",
        "firstName",
        "lastName"
})

public class Artist
{
    protected ArtType artType;
    protected String famousWorks;
    protected String firstName;
    protected String lastName;


    public ArtType getArtType()
    {
        return artType;
    }

    public void setArtType(ArtType artType)
    {
        this.artType = artType;
    }

    public String getFamousWorks()
    {
        return famousWorks;
    }

    public void setFamousWorks(String famousWorks)
    {
        this.famousWorks = famousWorks;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
}
