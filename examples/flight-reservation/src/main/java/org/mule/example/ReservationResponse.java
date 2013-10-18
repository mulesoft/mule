/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
