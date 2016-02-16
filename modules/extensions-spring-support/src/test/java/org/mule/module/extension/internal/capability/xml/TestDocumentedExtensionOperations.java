/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.param.Ignore;

public class TestDocumentedExtensionOperations
{

    /**
     * Test Operation
     *
     * @param value test value
     */
    public void operation(String value, @ParameterGroup TestDocumentedParameterGroup group)
    {

    }

    /**
     * Test Operation with blank parameter description
     *
     * @param value
     */
    public void operationWithBlankParameterDescription(String value)
    {

    }

    /**
     * This operation should not be documented
     *
     * @param value test value
     */
    @Ignore
    public void ignoreOperationShouldBeIgnored(String value)
    {

    }

    /**
     * Private operation should not be documented
     *
     * @param value test value
     */
    private void privateOperationShouldBeIgnored(String value)
    {

    }
}
