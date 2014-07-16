/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
