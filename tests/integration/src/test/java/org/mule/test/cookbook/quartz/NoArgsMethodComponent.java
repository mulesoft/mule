/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

