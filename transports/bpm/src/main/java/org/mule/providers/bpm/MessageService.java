/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.bpm;

import org.mule.umo.UMOMessage;

import java.util.Map;

public interface MessageService {

    public UMOMessage generateMessage(String endpoint, Object payloadObject, Map messageProperties, boolean synchronous) throws Exception;
}
