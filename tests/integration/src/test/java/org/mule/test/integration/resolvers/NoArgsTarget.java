/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

public class NoArgsTarget
{

    public String notIgnored()
    {
        return "notIgnored";
    }

    public String unused()
    {
        return "unused";
    }

    public String selected()
    {
        return "selected";
    }

}
