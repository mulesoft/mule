/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

import org.mule.extensions.annotations.Parameters;

public class ExtendedPersonalInfo extends PersonalInfo
{
    @Parameters
    private LifetimeInfo lifetimeInfo = new LifetimeInfo();

    public LifetimeInfo getLifetimeInfo()
    {
        return lifetimeInfo;
    }

    public void setLifetimeInfo(LifetimeInfo lifetimeInfo)
    {
        this.lifetimeInfo = lifetimeInfo;
    }
}
