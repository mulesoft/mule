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


package org.mule.module.extension.internal.capability.xml.schema.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}openAttrs">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element ref="{http://www.w3.org/2001/XMLSchema}annotation"/>
 *         &lt;group ref="{http://www.w3.org/2001/XMLSchema}redefinable"/>
 *       &lt;/choice>
 *       &lt;attribute name="schemaLocation" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "annotationOrSimpleTypeOrComplexType"
})
@XmlRootElement(name = "redefine")
public class Redefine extends OpenAttrs
{

    @XmlElements({
                         @XmlElement(name = "simpleType", type = TopLevelSimpleType.class),
                         @XmlElement(name = "complexType", type = TopLevelComplexType.class),
                         @XmlElement(name = "attributeGroup", type = NamedAttributeGroup.class),
                         @XmlElement(name = "group", type = NamedGroup.class),
                         @XmlElement(name = "annotation", type = Annotation.class)
                 })
    protected List<OpenAttrs> annotationOrSimpleTypeOrComplexType;
    @XmlAttribute(name = "schemaLocation", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String schemaLocation;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    /**
     * Gets the value of the annotationOrSimpleTypeOrComplexType property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the annotationOrSimpleTypeOrComplexType property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnnotationOrSimpleTypeOrComplexType().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link TopLevelSimpleType }
     * {@link TopLevelComplexType }
     * {@link NamedAttributeGroup }
     * {@link NamedGroup }
     * {@link Annotation }
     */
    public List<OpenAttrs> getAnnotationOrSimpleTypeOrComplexType()
    {
        if (annotationOrSimpleTypeOrComplexType == null)
        {
            annotationOrSimpleTypeOrComplexType = new ArrayList<OpenAttrs>();
        }
        return this.annotationOrSimpleTypeOrComplexType;
    }

    /**
     * Gets the value of the schemaLocation property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    /**
     * Sets the value of the schemaLocation property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSchemaLocation(String value)
    {
        this.schemaLocation = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value)
    {
        this.id = value;
    }

}
