/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

import org.mule.extensions.annotations.ImplementationOf;
import org.mule.extensions.annotations.Operation;
import org.mule.extensions.annotations.Parameters;

public class HeisenbergAliasOperations
{

    @Parameters
    private PersonalInfo personalInfo;

    @Operation
    @ImplementationOf(HeisenbergExtension.class)
    public String alias()
    {
        return String.format("Hello, my name is %s. I'm %d years old", personalInfo.getMyName(), personalInfo.getAge());
    }

    public PersonalInfo getPersonalInfo()
    {
        return personalInfo;
    }

    public void setPersonalInfo(PersonalInfo personalInfo)
    {
        this.personalInfo = personalInfo;
    }
}
