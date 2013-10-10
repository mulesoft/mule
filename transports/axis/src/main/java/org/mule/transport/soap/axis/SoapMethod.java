/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.config.spring.editors.QNamePropertyEditor;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
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
    private List namedParameters = new ArrayList();
    private QName returnType;
    private Class returnClass = Object.class;

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
    public SoapMethod(String methodName, List params) throws ClassNotFoundException
    {
        this(QNamePropertyEditor.convert(methodName), params);
    }

    public SoapMethod(QName methodName, String paramsString) throws ClassNotFoundException
    {
        name = methodName;
        List params = new ArrayList();
        for (StringTokenizer stringTokenizer = new StringTokenizer(paramsString, ","); stringTokenizer.hasMoreTokens();)
        {
            params.add(stringTokenizer.nextToken().trim());
        }
        initParams(params);
    }

    public SoapMethod(QName methodName, List params) throws ClassNotFoundException
    {
        name = methodName;
        initParams(params);
    }

    private void initParams(List params) throws ClassNotFoundException
    {

        NamedParameter param;
        for (Iterator iterator = params.iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();

            for (StringTokenizer tokenizer = new StringTokenizer(s, ";"); tokenizer.hasMoreTokens();)
            {
                String name = tokenizer.nextToken();
                String type = tokenizer.nextToken();
                if (name.equalsIgnoreCase("return"))
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
                else if (name.equalsIgnoreCase("returnClass"))
                {
                    returnClass = ClassUtils.loadClass(type, getClass());
                }
                else
                {
                    String mode = tokenizer.nextToken();
                    QName paramName;
                    if (name.startsWith("qname{"))
                    {
                        paramName = QNamePropertyEditor.convert(name);
                    }
                    else
                    {
                        paramName = new QName(getName().getNamespaceURI(), name, getName().getPrefix());
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
                    param = new NamedParameter(paramName, qtype, mode);
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

    public SoapMethod(QName name, QName returnType, Class returnClass)
    {
        this.name = name;
        this.returnType = returnType;
        this.returnClass = returnClass;
    }

    public SoapMethod(QName name, Class returnClass)
    {
        this.name = name;
        this.returnClass = returnClass;
    }

    public SoapMethod(QName name, List namedParameters, QName returnType)
    {
        this.name = name;
        this.namedParameters = namedParameters;
        this.returnType = returnType;
    }

    public void addNamedParameter(NamedParameter param)
    {
        namedParameters.add(param);
    }

    public NamedParameter addNamedParameter(QName name, QName type, String mode)
    {
        if (StringUtils.isBlank(name.getNamespaceURI()))
        {
            name = new QName(getName().getNamespaceURI(), name.getLocalPart(), name.getPrefix());
        }
        NamedParameter param = new NamedParameter(name, type, mode);
        namedParameters.add(param);
        return param;
    }

    public NamedParameter addNamedParameter(QName name, QName type, ParameterMode mode)
    {
        if (StringUtils.isBlank(name.getNamespaceURI()))
        {
            name = new QName(getName().getNamespaceURI(), name.getLocalPart(), name.getPrefix());
        }
        NamedParameter param = new NamedParameter(name, type, mode);
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

    public List getNamedParameters()
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

    public Class getReturnClass()
    {
        return returnClass;
    }

    public void setReturnDataType(Class returnClass)
    {
        this.returnClass = returnClass;
    }
}
