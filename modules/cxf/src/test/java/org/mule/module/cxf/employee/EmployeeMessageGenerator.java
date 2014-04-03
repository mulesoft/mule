/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.employee;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.example.employee.Employee;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class EmployeeMessageGenerator implements Callable
{
    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        Employee employee = new Employee();
        employee.setName("Albert Einstein");
        employee.setDivision("Theoretical Physics");
        employee.setPicture(new DataHandler(new FileDataSource("src/test/resources/albert_einstein.jpg")));
        return employee;
    }
}
