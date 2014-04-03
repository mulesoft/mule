/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.employee;

import org.mule.example.employee.Employee;
import org.mule.example.employee.EmployeeDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

@WebService(serviceName = "EmployeeDirectory", portName = "EmployeeDirectoryPort", endpointInterface = "org.mule.example.employee.EmployeeDirectory")
public class EmployeeDirectoryImpl implements EmployeeDirectory
{

    private volatile int invocationCount;
    private List<Employee> employees = new ArrayList<Employee>();

    public List<Employee> getEmployees()
    {
        return employees;
    }

    public void addEmployee(Employee employee)
    {
        // Read the picture, otherwise the other side never finishes writing
        try
        {
            InputStream is = employee.getPicture().getInputStream();
            
            while (is.read() != -1);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        
        
        System.out.println("Added " + employee.getName() + " in division " + employee.getDivision()
                           + " with a picture " + employee.getPicture());
        employees.add(employee);
        invocationCount++;
    }

    public int getInvocationCount()
    {
        return invocationCount;
    }

}
