/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.xml_security;

public class CustomerServiceImpl implements CustomerService
{
    @Override
    public Customer updateCustomer(Customer customer)
    {
        return customer;
    }

    @Override
    public Customer getCustomer(String index)
    {
        Customer customer = new Customer();

        customer.setfName("FIRST NAME");
        customer.setlName("LAST NAME");

        return customer;
    }
}
