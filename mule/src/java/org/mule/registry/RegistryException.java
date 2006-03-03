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

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * Base Registry exception thrown when reading or writing to the registry
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RegistryException extends UMOException {
    public RegistryException(Message message) {
        super(message);
    }

    public RegistryException(Message message, Throwable throwable) {
        super(message, throwable);
    }

    public RegistryException(Throwable throwable) {
        super(throwable);
    }

    //TODO remove
    public RegistryException(String message) {
        super(Message.createStaticMessage(message));
    }

    public RegistryException(String message, Throwable throwable) {
        super(Message.createStaticMessage(message), throwable);
    }
}
