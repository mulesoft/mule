/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cxf.weatherservice.myweather;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for POP complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="POP">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Nighttime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Daytime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "POP", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "nighttime",
    "daytime"
})
public class POP {

    @XmlElement(name = "Nighttime", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String nighttime;
    @XmlElement(name = "Daytime", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String daytime;

    /**
     * Gets the value of the nighttime property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getNighttime() {
        return nighttime;
    }

    /**
     * Sets the value of the nighttime property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setNighttime(String value) {
        this.nighttime = value;
    }

    /**
     * Gets the value of the daytime property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDaytime() {
        return daytime;
    }

    /**
     * Sets the value of the daytime property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDaytime(String value) {
        this.daytime = value;
    }

}