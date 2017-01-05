/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.employee;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.mule.example.employee package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

  private final static QName _AddEmployee_QNAME = new QName("http://employee.example.mule.org/", "addEmployee");
  private final static QName _GetEmployeesResponse_QNAME = new QName("http://employee.example.mule.org/", "getEmployeesResponse");
  private final static QName _AddEmployeeResponse_QNAME = new QName("http://employee.example.mule.org/", "addEmployeeResponse");
  private final static QName _GetEmployees_QNAME = new QName("http://employee.example.mule.org/", "getEmployees");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.mule.example.employee
   * 
   */
  public ObjectFactory() {}

  /**
   * Create an instance of {@link AddEmployee }
   *
   */
  public AddEmployee createAddEmployee() {
    return new AddEmployee();
  }

  /**
   * Create an instance of {@link GetEmployeesResponse }
   *
   */
  public GetEmployeesResponse createGetEmployeesResponse() {
    return new GetEmployeesResponse();
  }

  /**
   * Create an instance of {@link AddEmployeeResponse }
   *
   */
  public AddEmployeeResponse createAddEmployeeResponse() {
    return new AddEmployeeResponse();
  }

  /**
   * Create an instance of {@link Employee }
   *
   */
  public Employee createEmployee() {
    return new Employee();
  }

  /**
   * Create an instance of {@link GetEmployees }
   *
   */
  public GetEmployees createGetEmployees() {
    return new GetEmployees();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link AddEmployee }{@code >}}
   *
   */
  @XmlElementDecl(namespace = "http://employee.example.mule.org/", name = "addEmployee")
  public JAXBElement<AddEmployee> createAddEmployee(AddEmployee value) {
    return new JAXBElement<AddEmployee>(_AddEmployee_QNAME, AddEmployee.class, null, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link GetEmployeesResponse }{@code >}}
   *
   */
  @XmlElementDecl(namespace = "http://employee.example.mule.org/", name = "getEmployeesResponse")
  public JAXBElement<GetEmployeesResponse> createGetEmployeesResponse(GetEmployeesResponse value) {
    return new JAXBElement<GetEmployeesResponse>(_GetEmployeesResponse_QNAME, GetEmployeesResponse.class, null, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link AddEmployeeResponse }{@code >}}
   *
   */
  @XmlElementDecl(namespace = "http://employee.example.mule.org/", name = "addEmployeeResponse")
  public JAXBElement<AddEmployeeResponse> createAddEmployeeResponse(AddEmployeeResponse value) {
    return new JAXBElement<AddEmployeeResponse>(_AddEmployeeResponse_QNAME, AddEmployeeResponse.class, null, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link GetEmployees }{@code >}}
   *
   */
  @XmlElementDecl(namespace = "http://employee.example.mule.org/", name = "getEmployees")
  public JAXBElement<GetEmployees> createGetEmployees(GetEmployees value) {
    return new JAXBElement<GetEmployees>(_GetEmployees_QNAME, GetEmployees.class, null, value);
  }

}
