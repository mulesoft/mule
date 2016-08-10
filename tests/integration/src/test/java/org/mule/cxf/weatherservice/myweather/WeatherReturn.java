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
 * <p>Java class for WeatherReturn complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="WeatherReturn">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Success" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ResponseText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="State" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="City" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="WeatherStationCity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="WeatherID" type="{http://www.w3.org/2001/XMLSchema}short"/>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Temperature" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="RelativeHumidity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Wind" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Pressure" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Visibility" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="WindChill" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Remarks" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherReturn", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "success",
    "responseText",
    "state",
    "city",
    "weatherStationCity",
    "weatherID",
    "description",
    "temperature",
    "relativeHumidity",
    "wind",
    "pressure",
    "visibility",
    "windChill",
    "remarks"
})
public class WeatherReturn {

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
    @XmlElement(name = "WeatherID", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected short weatherID;
    @XmlElement(name = "Description", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String description;
    @XmlElement(name = "Temperature", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String temperature;
    @XmlElement(name = "RelativeHumidity", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String relativeHumidity;
    @XmlElement(name = "Wind", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String wind;
    @XmlElement(name = "Pressure", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String pressure;
    @XmlElement(name = "Visibility", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String visibility;
    @XmlElement(name = "WindChill", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String windChill;
    @XmlElement(name = "Remarks", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String remarks;

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
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the temperature property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * Sets the value of the temperature property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTemperature(String value) {
        this.temperature = value;
    }

    /**
     * Gets the value of the relativeHumidity property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRelativeHumidity() {
        return relativeHumidity;
    }

    /**
     * Sets the value of the relativeHumidity property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRelativeHumidity(String value) {
        this.relativeHumidity = value;
    }

    /**
     * Gets the value of the wind property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getWind() {
        return wind;
    }

    /**
     * Sets the value of the wind property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setWind(String value) {
        this.wind = value;
    }

    /**
     * Gets the value of the pressure property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPressure() {
        return pressure;
    }

    /**
     * Sets the value of the pressure property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPressure(String value) {
        this.pressure = value;
    }

    /**
     * Gets the value of the visibility property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * Sets the value of the visibility property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setVisibility(String value) {
        this.visibility = value;
    }

    /**
     * Gets the value of the windChill property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getWindChill() {
        return windChill;
    }

    /**
     * Sets the value of the windChill property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setWindChill(String value) {
        this.windChill = value;
    }

    /**
     * Gets the value of the remarks property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Sets the value of the remarks property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRemarks(String value) {
        this.remarks = value;
    }

}