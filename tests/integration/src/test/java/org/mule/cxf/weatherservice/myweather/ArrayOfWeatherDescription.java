package org.mule.cxf.weatherservice.myweather;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ArrayOfWeatherDescription complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ArrayOfWeatherDescription">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="WeatherDescription" type="{http://ws.cdyne.com/WeatherWS/}WeatherDescription" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfWeatherDescription", namespace = "http://ws.cdyne.com/WeatherWS/", propOrder = {
    "weatherDescription"
})
public class ArrayOfWeatherDescription {

    @XmlElement(name = "WeatherDescription", namespace = "http://ws.cdyne.com/WeatherWS/")
    protected List<WeatherDescription> weatherDescription;

    /**
     * Gets the value of the weatherDescription property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the weatherDescription property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWeatherDescription().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WeatherDescription }
     *
     *
     */
    public List<WeatherDescription> getWeatherDescription() {
        if (weatherDescription == null) {
            weatherDescription = new ArrayList<WeatherDescription>();
        }
        return this.weatherDescription;
    }

}