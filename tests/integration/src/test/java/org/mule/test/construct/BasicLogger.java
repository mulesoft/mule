/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

public class BasicLogger implements Callable
{
    private String type;
    
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public Object onCall( MuleEventContext eventContext ) throws Exception 
    {
        return eventContext.getMessage();
    }
}