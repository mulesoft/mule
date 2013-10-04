/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.cookbook.quartz;

/**
 * A service that just requires its method to be invoked by Mule without an event
 */
// START SNIPPET: full-class
public class NoArgsMethodComponent
{
    public String fireMe()
    {
        return "Bullseye!";
    }
}
// END SNIPPET: full-class

