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


package org.mule.module.extensions.internal.capability.xml.schema.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for restrictionType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="restrictionType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated">
 *       &lt;sequence>
 *         &lt;choice minOccurs="0">
 *           &lt;group ref="{http://www.w3.org/2001/XMLSchema}typeDefParticle"/>
 *           &lt;group ref="{http://www.w3.org/2001/XMLSchema}simpleRestrictionModel"/>
 *         &lt;/choice>
 *         &lt;group ref="{http://www.w3.org/2001/XMLSchema}attrDecls"/>
 *       &lt;/sequence>
 *       &lt;attribute name="base" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "restrictionType", propOrder = {
        "group",
        "all",
        "choice",
        "sequence",
        "simpleType",
        "facets",
        "attributeOrAttributeGroup",
        "anyAttribute"
})
@XmlSeeAlso({
        ComplexRestrictionType.class,
        SimpleRestrictionType.class
})
public class RestrictionType extends Annotated {

    protected GroupRef group;
    protected All all;
    protected ExplicitGroup choice;
    protected ExplicitGroup sequence;
    protected LocalSimpleType simpleType;
    @XmlElementRefs({
            @XmlElementRef(name = "maxInclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "minLength", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "minInclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "totalDigits", namespace = "http://www.w3.org/2001/XMLSchema", type = TotalDigits.class),
            @XmlElementRef(name = "pattern", namespace = "http://www.w3.org/2001/XMLSchema", type = Pattern.class),
            @XmlElementRef(name = "minExclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "fractionDigits", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "enumeration", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "whiteSpace", namespace = "http://www.w3.org/2001/XMLSchema", type = WhiteSpace.class),
            @XmlElementRef(name = "maxLength", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "maxExclusive", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class),
            @XmlElementRef(name = "length", namespace = "http://www.w3.org/2001/XMLSchema", type = JAXBElement.class)
    })
    protected List<Object> facets;
    @XmlElements({
            @XmlElement(name = "attributeGroup", type = AttributeGroupRef.class),
            @XmlElement(name = "attribute", type = Attribute.class)
    })
    protected List<Annotated> attributeOrAttributeGroup;
    protected Wildcard anyAttribute;
    @XmlAttribute(name = "base", required = true)
    protected QName base;

    /**
     * Gets the value of the group property.
     *
     * @return possible object is
     *         {@link GroupRef }
     */
    public GroupRef getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     *
     * @param value allowed object is
     *              {@link GroupRef }
     */
    public void setGroup(GroupRef value) {
        this.group = value;
    }

    /**
     * Gets the value of the all property.
     *
     * @return possible object is
     *         {@link All }
     */
    public All getAll() {
        return all;
    }

    /**
     * Sets the value of the all property.
     *
     * @param value allowed object is
     *              {@link All }
     */
    public void setAll(All value) {
        this.all = value;
    }

    /**
     * Gets the value of the choice property.
     *
     * @return possible object is
     *         {@link ExplicitGroup }
     */
    public ExplicitGroup getChoice() {
        return choice;
    }

    /**
     * Sets the value of the choice property.
     *
     * @param value allowed object is
     *              {@link ExplicitGroup }
     */
    public void setChoice(ExplicitGroup value) {
        this.choice = value;
    }

    /**
     * Gets the value of the sequence property.
     *
     * @return possible object is
     *         {@link ExplicitGroup }
     */
    public ExplicitGroup getSequence() {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     *
     * @param value allowed object is
     *              {@link ExplicitGroup }
     */
    public void setSequence(ExplicitGroup value) {
        this.sequence = value;
    }

    /**
     * Gets the value of the simpleType property.
     *
     * @return possible object is
     *         {@link LocalSimpleType }
     */
    public LocalSimpleType getSimpleType() {
        return simpleType;
    }

    /**
     * Sets the value of the simpleType property.
     *
     * @param value allowed object is
     *              {@link LocalSimpleType }
     */
    public void setSimpleType(LocalSimpleType value) {
        this.simpleType = value;
    }

    /**
     * Gets the value of the facets property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the facets property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFacets().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link TotalDigits }
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link Pattern }
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     * {@link JAXBElement }{@code <}{@link NoFixedFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link WhiteSpace }
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link NumFacet }{@code >}
     * {@link JAXBElement }{@code <}{@link Facet }{@code >}
     */
    public List<Object> getFacets() {
        if (facets == null) {
            facets = new ArrayList<Object>();
        }
        return this.facets;
    }

    /**
     * Gets the value of the attributeOrAttributeGroup property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attributeOrAttributeGroup property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttributeOrAttributeGroup().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeGroupRef }
     * {@link Attribute }
     */
    public List<Annotated> getAttributeOrAttributeGroup() {
        if (attributeOrAttributeGroup == null) {
            attributeOrAttributeGroup = new ArrayList<Annotated>();
        }
        return this.attributeOrAttributeGroup;
    }

    /**
     * Gets the value of the anyAttribute property.
     *
     * @return possible object is
     *         {@link Wildcard }
     */
    public Wildcard getAnyAttribute() {
        return anyAttribute;
    }

    /**
     * Sets the value of the anyAttribute property.
     *
     * @param value allowed object is
     *              {@link Wildcard }
     */
    public void setAnyAttribute(Wildcard value) {
        this.anyAttribute = value;
    }

    /**
     * Gets the value of the base property.
     *
     * @return possible object is
     *         {@link QName }
     */
    public QName getBase() {
        return base;
    }

    /**
     * Sets the value of the base property.
     *
     * @param value allowed object is
     *              {@link QName }
     */
    public void setBase(QName value) {
        this.base = value;
    }

}
