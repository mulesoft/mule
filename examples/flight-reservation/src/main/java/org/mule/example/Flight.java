/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example;

import java.io.Serializable;

public class Flight implements Serializable
{
	private String flightNumber;
	private String seatInfo;
	private Double ticketPrice;
	
	public Double getTicketPrice()
    {
		return ticketPrice;
	}

	public void setTicketPrice(Double ticketPrice)
    {
		this.ticketPrice = ticketPrice;
	}

	public void setFlightNumber(String flightNumber)
	{
		this.flightNumber = flightNumber;
	}
	
	public String getFlightNumber()
	{
		return this.flightNumber;
	}
	
	public void setSeatInfo(String seatInfo)
	{
		this.seatInfo = seatInfo;
	}
	
	public String getSeatInfo()
	{
		return this.seatInfo;
	}
}
