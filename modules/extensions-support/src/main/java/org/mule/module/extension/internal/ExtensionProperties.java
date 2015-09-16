/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.mule.api.config.MuleProperties.PROPERTY_PREFIX;

/**
 * Constants for the Extensions Framework
 *
 * @since 4.0
 */
public class ExtensionProperties
{

    private ExtensionProperties()
    {
    }

    /**
     * The key of an operation's parameter on which the connection to be used was set
     */
    public static final String CONNECTION_PARAM = PROPERTY_PREFIX + "CONNECTION_PARAM";

}
