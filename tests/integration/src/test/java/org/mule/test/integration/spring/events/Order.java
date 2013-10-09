/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.spring.events;

import java.io.Serializable;

/**
 * <code>Order</code> is a bean that gets sent back and forth between Mule and
 * Spring.
 */

public class Order implements Serializable
{
    private static final long serialVersionUID = -5384677758697949102L;

    private String order;

    public Order()
    {
        super();
    }

    public Order(String order)
    {
        this.order = order;
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

}
