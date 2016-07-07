/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import org.mule.runtime.api.message.Attributes;

/**
 * Default implementation of {@link Attributes} to be used when no other connector specific attributes instance is set.
 */
public final class NullAttributes implements Attributes
{

    private static final long serialVersionUID = 1201393762712713465L;

    @Override
    public String toString()
    {
        return "{NullAttributes}";
    }
}
