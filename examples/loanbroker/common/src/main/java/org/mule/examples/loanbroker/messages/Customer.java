/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.messages;

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
