/*
 * $Id: ApplicationEventException.java 2179 2006-06-04 22:51:52Z holger $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.events;

/**
 * <code>ApplicationEventException</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 2179 $
 */

public class ApplicationEventException extends Exception
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 718759087364948708L;

    public ApplicationEventException(String message)
    {
        super(message);
    }

    public ApplicationEventException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
