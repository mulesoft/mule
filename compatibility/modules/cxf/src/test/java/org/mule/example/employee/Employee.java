/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.employee;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for employee complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="employee">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="division" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="picture" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "employee", propOrder = {
    "division",
    "name",
    "picture"
})
public class Employee {

  protected String division;
  protected String name;
  @XmlMimeType("application/octet-stream")
  protected DataHandler picture;

  /**
   * Gets the value of the division property.
   * 
   * @return
   *     possible object is
   *     {@link String }
   *     
   */
  public String getDivision() {
    return division;
  }

  /**
   * Sets the value of the division property.
   * 
   * @param value
   *     allowed object is
   *     {@link String }
   *     
   */
  public void setDivision(String value) {
    this.division = value;
  }

  /**
   * Gets the value of the name property.
   * 
   * @return
   *     possible object is
   *     {@link String }
   *     
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of the name property.
   * 
   * @param value
   *     allowed object is
   *     {@link String }
   *     
   */
  public void setName(String value) {
    this.name = value;
  }

  /**
   * Gets the value of the picture property.
   * 
   * @return
   *     possible object is
   *     {@link DataHandler }
   *     
   */
  public DataHandler getPicture() {
    return picture;
  }

  /**
   * Sets the value of the picture property.
   * 
   * @param value
   *     allowed object is
   *     {@link DataHandler }
   *     
   */
  public void setPicture(DataHandler value) {
    this.picture = value;
  }

}
