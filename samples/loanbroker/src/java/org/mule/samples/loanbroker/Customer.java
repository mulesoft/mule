/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import java.io.Serializable;

/**
 * <code>Customer</code> the loan broker customer
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Customer implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4831762346946636795L;

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
