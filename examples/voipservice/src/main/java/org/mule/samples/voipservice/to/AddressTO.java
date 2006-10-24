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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Binildas Christudas
 */
public class AddressTO implements Serializable, Cloneable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6721555269589989794L;

    private String houseNumber;
    private String street;
    private String city;

    private static final List ADDRESSES;

    static
    {
        ADDRESSES = new ArrayList();

        ADDRESSES.add(new AddressTO("123", "Koudiar Palace", "Trivandrum"));
        ADDRESSES.add(new AddressTO("222", "Lake View", "Cochin"));
        ADDRESSES.add(new AddressTO("345", "Spencer Town", "Chennai"));
        ADDRESSES.add(new AddressTO("898", "Electronics City", "Bangalore"));
        ADDRESSES.add(new AddressTO("554", "Kovalam Beach", "Trivandrum"));
        ADDRESSES.add(new AddressTO("101", "Anzyl Grove", "Pune"));
        ADDRESSES.add(new AddressTO("369", "Victoria Terminus", "Mumbai"));
        ADDRESSES.add(new AddressTO("876", "Ponmudi Hills", "Trivandrum"));
        ADDRESSES.add(new AddressTO("777", "White Field", "Bangalore"));
        ADDRESSES.add(new AddressTO("908", "Varkala Palms", "Trivandrum"));

    }

    public AddressTO()
    {
        super();
    }

    public AddressTO(String houseNumber, String street, String city)
    {

        this.houseNumber = houseNumber;
        this.street = street;
        this.city = city;
    }

    public void setHouseNumber(String houseNumber)
    {
        this.houseNumber = houseNumber;
    }

    public String getHouseNumber()
    {
        return houseNumber;
    }

    public void setStreet(String street)
    {
        this.street = street;
    }

    public String getStreet()
    {
        return street;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getCity()
    {
        return city;
    }

    public Object clone()
    {
        Object clone = null;
        try
        {
            clone = super.clone();
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
        if (this.houseNumber != null)
        {
            stringBuffer.append("[HouseNumber : " + houseNumber + "; ");
        }
        if (this.street != null)
        {
            stringBuffer.append("Street : " + street + "; ");
        }
        if (this.houseNumber != null)
        {
            stringBuffer.append("City : " + city + "]");
        }
        return stringBuffer.toString();
    }

    public static AddressTO getRandomAddress()
    {

        int index = new Double(Math.random() * 10).intValue();
        // AddressTO addressTO = (AddressTO) ADDRESSES.get(index);
        return (AddressTO)((AddressTO)ADDRESSES.get(index)).clone();
    }

}
