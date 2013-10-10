/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.services;

import java.rmi.Remote;

public class SimpleMathsComponent implements Remote, AdditionService
{

    public Integer addTen(Integer number)
    {
        return new Integer(number.intValue() + 10);
    }

    public int add(int[] args)
    {
        int result = 0;
        for (int i = 0; i < args.length; i++)
        {
            result += args[i];
        }
        return result;
    }
}
