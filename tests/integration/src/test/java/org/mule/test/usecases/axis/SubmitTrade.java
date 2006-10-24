/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis;

/**
 * SubmitTrade.java This file was auto-generated from WSDL by the Apache Axis 1.2.1
 * Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

public class SubmitTrade implements java.io.Serializable
{
    private static final long serialVersionUID = 6724911620302616166L;

    private org.mule.test.usecases.axis.Trade arg0;

    public SubmitTrade()
    {
        super();
    }

    public SubmitTrade(org.mule.test.usecases.axis.Trade arg0)
    {
        this.arg0 = arg0;
    }

    /**
     * Gets the arg0 value for this SubmitTrade.
     * 
     * @return arg0
     */
    public org.mule.test.usecases.axis.Trade getArg0()
    {
        return arg0;
    }

    /**
     * Sets the arg0 value for this SubmitTrade.
     * 
     * @param arg0
     */
    public void setArg0(org.mule.test.usecases.axis.Trade arg0)
    {
        this.arg0 = arg0;
    }

    private java.lang.Object __equalsCalc = null;

    public synchronized boolean equals(java.lang.Object obj)
    {
        if (!(obj instanceof SubmitTrade))
        {
            return false;
        }
        SubmitTrade other = (SubmitTrade)obj;
        if (this == obj)
        {
            return true;
        }
        if (__equalsCalc != null)
        {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && ((this.arg0 == null && other.getArg0() == null) || (this.arg0 != null && this.arg0.equals(other.getArg0())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;

    public synchronized int hashCode()
    {
        if (__hashCodeCalc)
        {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getArg0() != null)
        {
            _hashCode += getArg0().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(
        SubmitTrade.class, true);

    static
    {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://samples.mule.org/hello", "submitTrade"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("arg0");
        elemField.setXmlName(new javax.xml.namespace.QName("http://samples.mule.org/hello", "arg0"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://samples.mule.org/hello", "arg0"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc()
    {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType,
                                                                    java.lang.Class _javaType,
                                                                    javax.xml.namespace.QName _xmlType)
    {
        return new org.apache.axis.encoding.ser.BeanSerializer(_javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType,
                                                                        java.lang.Class _javaType,
                                                                        javax.xml.namespace.QName _xmlType)
    {
        return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType, _xmlType, typeDesc);
    }

}
