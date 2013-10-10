/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
