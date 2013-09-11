/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.support;

import org.mule.model.AbstractModel;

/**
 * TODO
 */
@Deprecated
public class InheritedModel extends AbstractModel
{
    public String getType()
    {
        return "inherited";
    }
    

    @Override
    public String getName()
    {
        return super.getName() + "#" + hashCode();
    }

    public String getParentName()
    {
        return super.getName();
    }

}
