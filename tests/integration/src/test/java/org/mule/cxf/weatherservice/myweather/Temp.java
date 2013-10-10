/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.cxf.weatherservice.myweather;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for temp complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="temp">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MorningLow" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DaytimeHigh" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "temp", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "morningLow",
    "daytimeHigh"
})
public class Temp {

    @XmlElement(name = "MorningLow", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String morningLow;
    @XmlElement(name = "DaytimeHigh", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String daytimeHigh;

    /**
     * Gets the value of the morningLow property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMorningLow() {
        return morningLow;
    }

    /**
     * Sets the value of the morningLow property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setMorningLow(String value) {
        this.morningLow = value;
    }

    /**
     * Gets the value of the daytimeHigh property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDaytimeHigh() {
        return daytimeHigh;
    }

    /**
     * Sets the value of the daytimeHigh property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDaytimeHigh(String value) {
        this.daytimeHigh = value;
    }

}