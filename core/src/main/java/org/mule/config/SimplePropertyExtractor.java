/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import java.util.List;
import java.util.Map;

import org.mule.umo.UMOMessage;
import org.mule.util.PropertiesUtils;

/**
 * Looks up the property on the message using the name given.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SimplePropertyExtractor implements PropertyExtractor {
    public Object getProperty(String name, UMOMessage message) {
        return message.getProperty(name);
    }

    public Map getProperties(List names, UMOMessage message) {
        return PropertiesUtils.getMessageProperties(names, message);
    }
}
