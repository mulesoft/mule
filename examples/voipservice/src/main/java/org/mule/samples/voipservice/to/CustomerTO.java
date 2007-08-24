/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.to;

import org.mule.samples.voipservice.LocaleMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomerTO implements Serializable, Cloneable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7760891283901332894L;

    private String firstName;
    private String lastName;
    private AddressTO addressTO;

    private static final List CUSTOMERS;

    static
    {
        CUSTOMERS = new ArrayList();

        CUSTOMERS.add(new CustomerTO("Binil", "Das"));
        CUSTOMERS.add(new CustomerTO("Rajesh", "Warrier"));
        CUSTOMERS.add(new CustomerTO("Jacob", "Oommen"));
        CUSTOMERS.add(new CustomerTO("Shahanas", "Mohammed"));
        CUSTOMERS.add(new CustomerTO("Sowmya", "Hubert"));
        CUSTOMERS.add(new CustomerTO("Ann", "Binil"));
        CUSTOMERS.add(new CustomerTO("Rajesh", "Ravindran"));
        CUSTOMERS.add(new CustomerTO("Renjit", "Hubert"));
        CUSTOMERS.add(new CustomerTO("Brijesh", "Deb"));
        CUSTOMERS.add(new CustomerTO("Rama", "Varma"));
    }

    public CustomerTO()
    {
        super();
    }

    public CustomerTO(String firstName, String lastName)
    {
        this(firstName, lastName, null);
    }

    public CustomerTO(String firstName, String lastName, AddressTO addressTO)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.addressTO = addressTO;
    }

    public String getName()
    {

        String name = firstName;
        String lastName = null;
        if (this.lastName == null)
        {
            lastName = "";
        }
        else
        {
            lastName = this.lastName;
        }
        return name + " " + lastName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setAddress(AddressTO addressTO)
    {
        this.addressTO = addressTO;
    }

    public AddressTO getAddress()
    {
        return addressTO;
    }

    public Object clone()
    {
        Object clone = null;
        try
        {
            clone = super.clone();
            if (null != addressTO)
            {
                ((CustomerTO)clone).setAddress((AddressTO)addressTO.clone());
            }
        }
        catch (CloneNotSupportedException cloneNotSupportedException)
        {
            // too bad
        }
        return clone;
    }

    public String toString()
    {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.firstName != null)
        {
            stringBuffer.append(LocaleMessage.getFirstNameCaption(getName()));
        }
        if (this.addressTO != null)
        {
            stringBuffer.append(LocaleMessage.getAddressCaption(addressTO));
        }
        return stringBuffer.toString();
    }

    public static CustomerTO getRandomCustomer()
    {

        int index = new Double(Math.random() * 10).intValue();
        // AddressTO addressTO = (AddressTO) ADDRESSES.get(index);
        CustomerTO customerTO = (CustomerTO)((CustomerTO)CUSTOMERS.get(index)).clone();
        if (null == customerTO.getAddress())
        {
            customerTO.setAddress(AddressTO.getRandomAddress());
        }
        return customerTO;
    }

}
