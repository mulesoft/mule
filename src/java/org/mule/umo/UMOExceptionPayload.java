/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo;

import java.util.Map;

/**
 * <code>UMOExceptionPayload</code> is a message payload that contains exception information
 * that occurred during message processing
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOExceptionPayload {

    public int getCode();

    public String getMessage();

    public Map getInfo();

    public Throwable getException();
}
