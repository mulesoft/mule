/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

import static org.mule.module.extensions.HeisenbergExtension.AGE;
import static org.mule.module.extensions.HeisenbergExtension.HEISENBERG;
import org.mule.extensions.annotations.Parameter;
import org.mule.extensions.annotations.param.Optional;

public class PersonalInfo
{

    @Parameter
    @Optional(defaultValue = HEISENBERG)
    private String myName;

    @Parameter
    @Optional(defaultValue = AGE)
    private Integer age;

    public String getMyName()
    {
        return myName;
    }

    public void setMyName(String myName)
    {
        this.myName = myName;
    }

    public Integer getAge()
    {
        return age;
    }

    public void setAge(Integer age)
    {
        this.age = age;
    }
}
