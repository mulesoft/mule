/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example;

import java.io.Serializable;
import java.util.List;

public class ReservationRequest implements Serializable
{
	private Flight[] flights;

	public Flight[] getFlights()
    {
		return flights;
	}

	public void setFlights(Flight[] flights)
    {
		this.flights = flights;
	}

	
}
