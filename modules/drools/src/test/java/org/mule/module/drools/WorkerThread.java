/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.drools;

import org.drools.common.DefaultFactHandle;
import org.mule.module.bpm.Rules;

public class WorkerThread implements Runnable
{

    private Drools drools;
    private Rules rules;
    private Data data;

    WorkerThread(Data data, Drools drools, Rules rules)
    {
        this.drools = drools;
        this.data = data;
        this.rules = rules;
    }

    @Override
    public void run()
    {
        DefaultFactHandle handle;
        try
        {
            handle = (DefaultFactHandle) drools.assertFact(rules, this.getData());
            this.setData((Data) handle.getObject());
        }
        catch (Exception e)
        {

        }
    }

    public Data getData()
    {
        return data;
    }

    public void setData(Data data)
    {
        this.data = data;
    }

}
