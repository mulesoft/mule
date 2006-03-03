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
*
*/
package org.mule.registry;

import org.apache.commons.lang.SystemUtils;
import org.mule.config.i18n.Message;

import java.util.Iterator;
import java.util.List;

/**
 * Is thrown when an object loaded from a descriptor is invalid
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ValidationException extends RegistryException {
    public ValidationException(String vaildationError) {
        super(Message.createStaticMessage(vaildationError));
    }

    public ValidationException(List vaildationErrors) {
        this(createMessage(vaildationErrors));
    }

    private static String createMessage(List vaildationErrors) {
        //TODO i18n
        StringBuffer buf = new StringBuffer();
        buf.append("The following validation errors occurred");
        for (Iterator iterator = vaildationErrors.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            buf.append(SystemUtils.LINE_SEPARATOR).append(s);
        }
        return buf.toString();
    }
}
