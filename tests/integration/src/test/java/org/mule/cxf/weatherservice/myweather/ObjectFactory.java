/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cxf.weatherservice.myweather;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.myweather package.
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

    private final static QName _WeatherReturn_QNAME = new QName("http://ws.cdyne.com/WeatherWS/", "WeatherReturn");
    private final static QName _ArrayOfWeatherDescription_QNAME = new QName("http://ws.cdyne.com/WeatherWS/", "ArrayOfWeatherDescription");
    private final static QName _ForecastReturn_QNAME = new QName("http://ws.cdyne.com/WeatherWS/", "ForecastReturn");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.myweather
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link org.mule.cxf.weatherservice.myweather.GetCityForecastByZIP }
     *
     */
    public GetCityForecastByZIP createGetCityForecastByZIP() {
        return new GetCityForecastByZIP();
    }

    /**
     * Create an instance of {@link GetCityForecastByZIPResponse }
     *
     */
    public GetCityForecastByZIPResponse createGetCityForecastByZIPResponse() {
        return new GetCityForecastByZIPResponse();
    }

    /**
     * Create an instance of {@link POP }
     *
     */
    public POP createPOP() {
        return new POP();
    }

    /**
     * Create an instance of {@link WeatherDescription }
     *
     */
    public WeatherDescription createWeatherDescription() {
        return new WeatherDescription();
    }

    /**
     * Create an instance of {@link ArrayOfWeatherDescription }
     *
     */
    public ArrayOfWeatherDescription createArrayOfWeatherDescription() {
        return new ArrayOfWeatherDescription();
    }

    /**
     * Create an instance of {@link GetWeatherInformation }
     *
     */
    public GetWeatherInformation createGetWeatherInformation() {
        return new GetWeatherInformation();
    }

    /**
     * Create an instance of {@link GetCityWeatherByZIPResponse }
     *
     */
    public GetCityWeatherByZIPResponse createGetCityWeatherByZIPResponse() {
        return new GetCityWeatherByZIPResponse();
    }

    /**
     * Create an instance of {@link WeatherReturn }
     *
     */
    public WeatherReturn createWeatherReturn() {
        return new WeatherReturn();
    }

    /**
     * Create an instance of {@link org.mule.cxf.weatherservice.myweather.GetCityWeatherByZIP }
     *
     */
    public GetCityWeatherByZIP createGetCityWeatherByZIP() {
        return new GetCityWeatherByZIP();
    }

    /**
     * Create an instance of {@link Forecast }
     *
     */
    public Forecast createForecast() {
        return new Forecast();
    }

    /**
     * Create an instance of {@link Temp }
     *
     */
    public Temp createTemp() {
        return new Temp();
    }

    /**
     * Create an instance of {@link ForecastReturn }
     *
     */
    public ForecastReturn createForecastReturn() {
        return new ForecastReturn();
    }

    /**
     * Create an instance of {@link ArrayOfForecast }
     *
     */
    public ArrayOfForecast createArrayOfForecast() {
        return new ArrayOfForecast();
    }

    /**
     * Create an instance of {@link GetWeatherInformationResponse }
     *
     */
    public GetWeatherInformationResponse createGetWeatherInformationResponse() {
        return new GetWeatherInformationResponse();
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link WeatherReturn }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ws.cdyne.com/WeatherWS/", name = "WeatherReturn")
    public JAXBElement<WeatherReturn> createWeatherReturn(WeatherReturn value) {
        return new JAXBElement<WeatherReturn>(_WeatherReturn_QNAME, WeatherReturn.class, null, value);
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link ArrayOfWeatherDescription }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ws.cdyne.com/WeatherWS/", name = "ArrayOfWeatherDescription")
    public JAXBElement<ArrayOfWeatherDescription> createArrayOfWeatherDescription(ArrayOfWeatherDescription value) {
        return new JAXBElement<ArrayOfWeatherDescription>(_ArrayOfWeatherDescription_QNAME, ArrayOfWeatherDescription.class, null, value);
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link ForecastReturn }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ws.cdyne.com/WeatherWS/", name = "ForecastReturn")
    public JAXBElement<ForecastReturn> createForecastReturn(ForecastReturn value) {
        return new JAXBElement<ForecastReturn>(_ForecastReturn_QNAME, ForecastReturn.class, null, value);
    }

}