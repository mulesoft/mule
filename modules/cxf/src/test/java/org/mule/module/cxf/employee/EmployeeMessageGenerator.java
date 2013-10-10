/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.employee;

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
