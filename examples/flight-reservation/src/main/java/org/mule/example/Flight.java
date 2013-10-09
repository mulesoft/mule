/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
