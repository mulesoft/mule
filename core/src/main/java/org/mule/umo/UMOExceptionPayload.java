/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import java.util.Map;
import java.io.Serializable;

/**
 * <code>UMOExceptionPayload</code> is a message payload that contains exception
 * information that occurred during message processing
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOExceptionPayload extends Serializable
{

    int getCode();

    String getMessage();

    Map getInfo();

    Throwable getException();

    Throwable getRootException();

}
