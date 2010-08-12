/*
 * $Id: MtomClientTestCase.java 11678 2008-05-02 12:03:07Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cxf.employee;

import org.mule.example.employee.Employee;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class EmployeeMessageGenerator
{
    public Employee submitEmployee() throws Exception
    {
        Employee employee = new Employee();
        employee.setName("Albert Einstein");
        employee.setDivision("Theoretical Physics");
        employee.setPicture(new DataHandler(new FileDataSource("src/test/resources/albert_einstein.jpg")));
        return employee;
    }
}
