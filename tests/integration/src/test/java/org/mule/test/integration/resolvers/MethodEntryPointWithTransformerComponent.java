/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

public class MethodEntryPointWithTransformerComponent
{
    /**
     * Transforms a message for testing purposes.
     * <p>
     * Is referenced by the test configuration because it implements the test
     * component method which should be call by the MethodEntryPointResolver.
     */
    public String transformMessage(String message)
    {
        return "Transformed " + message;
    }
}
