/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        Map props = new HashMap();
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            props.put(s, getProperty(s, message));
        }
        return props;
    }
}
