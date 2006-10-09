/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.properties;

import org.mule.umo.UMOMessage;

/**
 * Looks up the property on the message using the name given.
 *
 */
public class SimplePropertyExtractor implements PropertyExtractor
{
    public Object getProperty(String name, UMOMessage message) {
        return message.getProperty(name);
    }
}
