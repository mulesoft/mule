/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.MuleMessage;

import java.util.List;

/**
 * An SPI interface where custom logic can be plugged in to control how collections and single messages
 * are returned from routers.
 */
public interface RouterResultsHandler
{
    MuleMessage aggregateResults(List /*<MuleMessage>*/ results, MuleMessage orginalMessage);
}
