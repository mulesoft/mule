/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import static org.mule.module.extension.HeisenbergExtension.AGE;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;

public class PersonalInfo
{

    @Parameter(alias = "myName")
    @Optional(defaultValue = HEISENBERG)
    private String name;

    @Parameter
    @Optional(defaultValue = AGE)
    private Integer age;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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
