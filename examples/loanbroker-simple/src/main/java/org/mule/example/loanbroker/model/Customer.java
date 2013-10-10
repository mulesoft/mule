/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.model;

import java.io.Serializable;

/**
 * <code>Customer</code> the loan broker customer
 */
public class Customer implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4622840173638021051L;

    private String name;

    private int ssn;

    public Customer()
    {
        super();
    }

    public Customer(String name, int ssn)
    {
        this.name = name;
        this.ssn = ssn;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getSsn()
    {
        return ssn;
    }

    public void setSsn(int ssn)
    {
        this.ssn = ssn;
    }

}
