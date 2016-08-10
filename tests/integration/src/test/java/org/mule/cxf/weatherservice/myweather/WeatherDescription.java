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
 * <p>Java class for WeatherDescription complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="WeatherDescription">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="WeatherID" type="{http://www.w3.org/2001/XMLSchema}short"/>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="PictureURL" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherDescription", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "weatherID",
    "description",
    "pictureURL"
})
public class WeatherDescription {

    @XmlElement(name = "WeatherID", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected short weatherID;
    @XmlElement(name = "Description", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String description;
    @XmlElement(name = "PictureURL", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected String pictureURL;

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
     * Gets the value of the pictureURL property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPictureURL() {
        return pictureURL;
    }

    /**
     * Sets the value of the pictureURL property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPictureURL(String value) {
        this.pictureURL = value;
    }

}