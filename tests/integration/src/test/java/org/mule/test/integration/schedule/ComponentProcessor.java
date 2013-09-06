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

    protected List<String> myCollection;

    public boolean process(String s)
    {

        synchronized (myCollection)
        {

            if (myCollection.size() < 10)
            {
                myCollection.add(s);
                return true;
            }
        }
        return false;
    }
}
