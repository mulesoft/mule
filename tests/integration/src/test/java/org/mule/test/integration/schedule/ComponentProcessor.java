/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import java.util.List;

public abstract class ComponentProcessor
{

    protected final List<String> myCollection;

    public ComponentProcessor(List<String> collection){
        myCollection = collection;
    }

    public final boolean process(String processMessage)
    {

        synchronized (myCollection)
        {

            if (myCollection.size() < 10)
            {
                myCollection.add(processMessage);
                return true;
            }
        }
        return false;
    }
}
