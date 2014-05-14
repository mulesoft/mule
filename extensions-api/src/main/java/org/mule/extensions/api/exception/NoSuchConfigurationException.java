/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.api.exception;

import org.mule.extensions.introspection.api.MuleExtension;

public class NoSuchConfigurationException extends Exception
{

    public NoSuchConfigurationException(MuleExtension muleExtension, String configName) {
        super(String.format("Configuration '%s' does not exists in Mule extension '%s'", configName, muleExtension.getName()));
    }
}
