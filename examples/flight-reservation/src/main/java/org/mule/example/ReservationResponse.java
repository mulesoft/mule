/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReservationResponse implements Serializable
{

	public List<String> errors = new ArrayList<String>();
	public Flight[] flights;
	public Double totalPrice;
	public Double getTotalPrice()
    {
		return totalPrice;
	}

	public void setTotalPrice(Double totalPrice)
    {
		this.totalPrice = totalPrice;
	}

	public ReservationRequest originalRequest;
	
	public Flight[] getFlights()
    {
		return flights;
	}

	public void setFlights(Flight[] flights)
    {
		this.flights = flights;
	}

	public void addError(String error)
	{
		errors.add(error);
	}
	
	public List<String> getErrors()
    {
		return errors;
	}

	public void setErrors(List<String> errors)
    {
		this.errors = errors;
	}

	public ReservationRequest getOriginalRequest()
    {
		return originalRequest;
	}

	public void setOriginalRequest(ReservationRequest originalRequest)
    {
		this.originalRequest = originalRequest;
	}

	
	
}
