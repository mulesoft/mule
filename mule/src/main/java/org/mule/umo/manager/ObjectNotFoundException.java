/* 
 * $Id$
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
package org.mule.umo.manager;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>ObjectNotFoundException</code> is thrown when a reference to a
 * component in a configured container is not found
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ObjectNotFoundException extends ContainerException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5012452325639544484L;

    /**
     * @param componentName the name of the component that could not be found
     */
    public ObjectNotFoundException(String componentName)
    {
        super(new Message(Messages.OBJECT_NOT_FOUND_X, componentName));
    }

    /**
     * @param componentName the name of the component that could not be found
     * @param cause the exception that cause this exception to be thrown
     */
    public ObjectNotFoundException(String componentName, Throwable cause)
    {
        super(new Message(Messages.OBJECT_NOT_FOUND_X, componentName), cause);
    }
}
