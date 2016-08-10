/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.2-hudson-jaxb-ri-2.2-63- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.14 at 03:58:12 PM GMT-03:00 
//


package org.mule.runtime.module.extension.internal.capability.xml.schema.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * itemType attribute and simpleType child are mutually
 * exclusive, but one or other is required
 * <p/>
 * <p/>
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated">
 *       &lt;sequence>
 *         &lt;element name="simpleType" type="{http://www.w3.org/2001/XMLSchema}localSimpleType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="itemType" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "simpleType"
})
@XmlRootElement(name = "list")
public class List extends Annotated
{

    protected LocalSimpleType simpleType;
    @XmlAttribute(name = "itemType")
    protected QName itemType;

    /**
     * Gets the value of the simpleType property.
     *
     * @return possible object is
     * {@link LocalSimpleType }
     */
    public LocalSimpleType getSimpleType()
    {
        return simpleType;
    }

    /**
     * Sets the value of the simpleType property.
     *
     * @param value allowed object is
     *              {@link LocalSimpleType }
     */
    public void setSimpleType(LocalSimpleType value)
    {
        this.simpleType = value;
    }

    /**
     * Gets the value of the itemType property.
     *
     * @return possible object is
     * {@link QName }
     */
    public QName getItemType()
    {
        return itemType;
    }

    /**
     * Sets the value of the itemType property.
     *
     * @param value allowed object is
     *              {@link QName }
     */
    public void setItemType(QName value)
    {
        this.itemType = value;
    }

}
