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
 * <p>Java class for ForecastReturn complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ForecastReturn">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Success" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ResponseText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="State" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="City" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="WeatherStationCity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ForecastResult" type="{http://ws.cdyne.com/WeatherWS/}ArrayOfForecast" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForecastReturn", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "success",
    "responseText",
    "state",
    "city",
    "weatherStationCity",
    "forecastResult"
})
public class ForecastReturn {

    @XmlElement(name = "Success", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected boolean success;
    @XmlElement(name = "ResponseText", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String responseText;
    @XmlElement(name = "State", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String state;
    @XmlElement(name = "City", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String city;
    @XmlElement(name = "WeatherStationCity", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String weatherStationCity;
    @XmlElement(name = "ForecastResult", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected ArrayOfForecast forecastResult;

    /**
     * Gets the value of the success property.
     *
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the value of the success property.
     *
     */
    public void setSuccess(boolean value) {
        this.success = value;
    }

    /**
     * Gets the value of the responseText property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getResponseText() {
        return responseText;
    }

    /**
     * Sets the value of the responseText property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setResponseText(String value) {
        this.responseText = value;
    }

    /**
     * Gets the value of the state property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setState(String value) {
        this.state = value;
    }

    /**
     * Gets the value of the city property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the value of the city property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCity(String value) {
        this.city = value;
    }

    /**
     * Gets the value of the weatherStationCity property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getWeatherStationCity() {
        return weatherStationCity;
    }

    /**
     * Sets the value of the weatherStationCity property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setWeatherStationCity(String value) {
        this.weatherStationCity = value;
    }

    /**
     * Gets the value of the forecastResult property.
     *
     * @return
     *     possible object is
     *     {@link ArrayOfForecast }
     *
     */
    public ArrayOfForecast getForecastResult() {
        return forecastResult;
    }

    /**
     * Sets the value of the forecastResult property.
     *
     * @param value
     *     allowed object is
     *     {@link ArrayOfForecast }
     *
     */
    public void setForecastResult(ArrayOfForecast value) {
        this.forecastResult = value;
    }

}