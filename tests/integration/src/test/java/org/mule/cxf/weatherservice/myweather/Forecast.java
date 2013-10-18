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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Forecast complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Forecast">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Date" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="WeatherID" type="{http://www.w3.org/2001/XMLSchema}short"/>
 *         &lt;element name="Desciption" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Temperatures" type="{http://ws.cdyne.com/WeatherWS/}temp"/>
 *         &lt;element name="ProbabilityOfPrecipiation" type="{http://ws.cdyne.com/WeatherWS/}POP"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Forecast", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "date",
    "weatherID",
    "desciption",
    "temperatures",
    "probabilityOfPrecipiation"
})
public class Forecast {

    @XmlElement(name = "Date", namespace = "http://ws.cdyne.com/WeatherWS/", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar date;
    @XmlElement(name = "WeatherID", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected short weatherID;
    @XmlElement(name = "Desciption", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String desciption;
    @XmlElement(name = "Temperatures", namespace = "http://ws.cdyne.com/WeatherWS/", required = true)
    protected Temp temperatures;
    @XmlElement(name = "ProbabilityOfPrecipiation", namespace = "http://ws.cdyne.com/WeatherWS/", required = true)
    protected POP probabilityOfPrecipiation;

    /**
     * Gets the value of the date property.
     *
     * @return
     *     possible object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     *
     * @param value
     *     allowed object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the weatherID property.
     *
     */
    public short getWeatherID() {
        return weatherID;
    }

    /**
     * Sets the value of the weatherID property.
     *
     */
    public void setWeatherID(short value) {
        this.weatherID = value;
    }

    /**
     * Gets the value of the desciption property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDesciption() {
        return desciption;
    }

    /**
     * Sets the value of the desciption property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDesciption(String value) {
        this.desciption = value;
    }

    /**
     * Gets the value of the temperatures property.
     *
     * @return
     *     possible object is
     *     {@link Temp }
     *
     */
    public Temp getTemperatures() {
        return temperatures;
    }

    /**
     * Sets the value of the temperatures property.
     *
     * @param value
     *     allowed object is
     *     {@link Temp }
     *
     */
    public void setTemperatures(Temp value) {
        this.temperatures = value;
    }

    /**
     * Gets the value of the probabilityOfPrecipiation property.
     *
     * @return
     *     possible object is
     *     {@link POP }
     *
     */
    public POP getProbabilityOfPrecipiation() {
        return probabilityOfPrecipiation;
    }

    /**
     * Sets the value of the probabilityOfPrecipiation property.
     *
     * @param value
     *     allowed object is
     *     {@link POP }
     *
     */
    public void setProbabilityOfPrecipiation(POP value) {
        this.probabilityOfPrecipiation = value;
    }

}