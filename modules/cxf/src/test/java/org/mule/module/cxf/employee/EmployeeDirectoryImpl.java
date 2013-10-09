/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    private int invocationCount;
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
