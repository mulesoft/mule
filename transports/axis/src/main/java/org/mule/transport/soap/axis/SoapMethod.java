/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.config.spring.editors.QNamePropertyEditor;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

/**
 * A SOAP method representation where the parameters are named
 */
public class SoapMethod
{
    private QName name;
    private List<NamedParameter> namedParameters = new ArrayList<NamedParameter>();
    private QName returnType;
    private Class<?> returnClass = Object.class;

    public SoapMethod(String methodName, String paramsString) throws ClassNotFoundException
    {
        this(QNamePropertyEditor.convert(methodName), paramsString);
    }

    /**
     * Creates a Soap Method using the param string set in the MUle configuration
     * file
     *
     * @param methodName the name of the method
     * @param params the param string to parse
     */
    public SoapMethod(String methodName, List<String> params) throws ClassNotFoundException
    {
        this(QNamePropertyEditor.convert(methodName), params);
    }

    public SoapMethod(QName methodName, String paramsString) throws ClassNotFoundException
    {
        name = methodName;
        List<String> params = new ArrayList<String>();
        for (StringTokenizer stringTokenizer = new StringTokenizer(paramsString, ","); stringTokenizer.hasMoreTokens();)
        {
            params.add(stringTokenizer.nextToken().trim());
        }
        initParams(params);
    }

    public SoapMethod(QName methodName, List<String> params) throws ClassNotFoundException
    {
        name = methodName;
        initParams(params);
    }

    private void initParams(List<String> params) throws ClassNotFoundException
    {
        for (String s : params)
        {
            for (StringTokenizer tokenizer = new StringTokenizer(s, ";"); tokenizer.hasMoreTokens();)
            {
                String qName = tokenizer.nextToken();
                String type = tokenizer.nextToken();
                if (qName.equalsIgnoreCase("return"))
                {
                    if (type.startsWith("qname{"))
                    {
                        returnType = QNamePropertyEditor.convert(type);
                    }
                    else
                    {
                        returnType = NamedParameter.createQName(type);
                    }
                }
                else if (qName.equalsIgnoreCase("returnClass"))
                {
                    returnClass = ClassUtils.loadClass(type, getClass());
                }
                else
                {
                    String mode = tokenizer.nextToken();
                    QName paramName;
                    if (qName.startsWith("qname{"))
                    {
                        paramName = QNamePropertyEditor.convert(qName);
                    }
                    else
                    {
                        paramName = new QName(getName().getNamespaceURI(), qName, getName().getPrefix());
                    }
                    QName qtype;
                    if (type.startsWith("qname{"))
                    {
                        qtype = QNamePropertyEditor.convert(type);
                    }
                    else
                    {
                        qtype = NamedParameter.createQName(type);
                    }
                    NamedParameter param = new NamedParameter(paramName, qtype, mode);
                    addNamedParameter(param);
                }
            }
        }
    }

    public SoapMethod(QName name)
    {
        this.name = name;
        this.returnType = null;
    }

    public SoapMethod(QName name, QName returnType)
    {
        this.name = name;
        this.returnType = returnType;
    }

    public SoapMethod(QName name, QName returnType, Class<?> returnClass)
    {
        this.name = name;
        this.returnType = returnType;
        this.returnClass = returnClass;
    }

    public SoapMethod(QName name, Class<?> returnClass)
    {
        this.name = name;
        this.returnClass = returnClass;
    }

    public SoapMethod(QName name, List<NamedParameter> namedParameters, QName returnType)
    {
        this.name = name;
        this.namedParameters = namedParameters;
        this.returnType = returnType;
    }

    public void addNamedParameter(NamedParameter param)
    {
        namedParameters.add(param);
    }

    public NamedParameter addNamedParameter(QName qName, QName type, String mode)
    {
        if (StringUtils.isBlank(qName.getNamespaceURI()))
        {
            qName = new QName(getName().getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
        }
        NamedParameter param = new NamedParameter(qName, type, mode);
        namedParameters.add(param);
        return param;
    }

    public NamedParameter addNamedParameter(QName qName, QName type, ParameterMode mode)
    {
        if (StringUtils.isBlank(qName.getNamespaceURI()))
        {
            qName = new QName(getName().getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
        }
        NamedParameter param = new NamedParameter(qName, type, mode);
        namedParameters.add(param);
        return param;
    }

    public void removeNamedParameter(NamedParameter param)
    {
        namedParameters.remove(param);
    }

    public QName getName()
    {
        return name;
    }

    public List<NamedParameter> getNamedParameters()
    {
        return namedParameters;
    }

    public QName getReturnType()
    {
        return returnType;
    }

    public void setReturnType(QName returnType)
    {
        this.returnType = returnType;
    }

    public Class<?> getReturnClass()
    {
        return returnClass;
    }

    public void setReturnDataType(Class<?> returnClass)
    {
        this.returnClass = returnClass;
    }
}
