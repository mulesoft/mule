/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.streaming;

/**
 * This enum lists the possible ways to navigate a streaming data feed.
 * 
 */
public enum StreamingOutputStrategy
{

    /**
     * This option means that the items should be returned one element at a time
     */
    ELEMENT,
    
    /**
     * This option means that the items should be grouped in pages
     */
    PAGE
}
