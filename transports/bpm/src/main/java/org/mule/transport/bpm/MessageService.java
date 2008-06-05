/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bpm;

import org.mule.api.MuleMessage;

import java.util.Map;

/**
 * A message-generation service provided by Mule.  Any BPMS may call this service from
 * its own processes in order to generate Mule messages.  The generated messages will 
 * be received by the "bpm://processName" endpoint within your Mule config.
 */
public interface MessageService {

    public MuleMessage generateMessage(String endpoint, Object payloadObject, Map messageProperties, boolean synchronous) throws Exception;
}
