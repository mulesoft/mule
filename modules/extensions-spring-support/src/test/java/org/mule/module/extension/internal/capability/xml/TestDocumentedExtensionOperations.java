/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.ParameterGroup;

public class TestDocumentedExtensionOperations
{

    /**
     * Test Operation
     *
     * @param value test value
     */
    @Operation
    public void operation(String value, @ParameterGroup TestDocumentedParameterGroup group) {

    }
}
