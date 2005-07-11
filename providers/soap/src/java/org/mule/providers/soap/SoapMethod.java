/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.soap;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.mule.util.ClassHelper;

/**
 * A soap method representation where the parameters are named
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapMethod {

    private String name;
    private List namedParameters = new ArrayList();
    private QName returnType;
    private Class returnClass;

    /**
     * Creates a Soap Method using the param string set in the MUle configuration
     * file
     * @param methodName the name of the method
     * @param params the param string to parse
     */
    public SoapMethod(String methodName, String params) throws ClassNotFoundException {
        name = methodName;
        NamedParameter param;
        for (StringTokenizer stringTokenizer = new StringTokenizer(params, ","); stringTokenizer.hasMoreTokens();) {
            String s = stringTokenizer.nextToken().trim();

            for (StringTokenizer tokenizer = new StringTokenizer(s, ":"); tokenizer.hasMoreTokens();) {
                String name = tokenizer.nextToken();
                String type = tokenizer.nextToken();
                if(name.equalsIgnoreCase("return")) {
                    returnType = NamedParameter.createQName(type);
                } else if(name.equalsIgnoreCase("returnClass")) {
                    returnClass = ClassHelper.loadClass(type, getClass());
                } else {
                    String mode = tokenizer.nextToken();
                    param = new NamedParameter(name, NamedParameter.createQName(type), mode);
                    addNamedParameter(param);
                }
            }
        }
    }

    public SoapMethod(String name) {
        this.name = name;
        this.returnType = null;
    }

    public SoapMethod(String name, QName returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public SoapMethod(String name, QName returnType, Class returnClass) {
        this.name = name;
        this.returnType = returnType;
        this.returnClass = returnClass;
    }

     public SoapMethod(String name, Class returnClass) {
        this.name = name;
        this.returnClass = returnClass;
    }

    public SoapMethod(String name, List namedParameters, QName returnType) {
        this.name = name;
        this.namedParameters = namedParameters;
        this.returnType = returnType;
    }

    public SoapMethod(String name, List namedParameters ) {
        this.name = name;
        this.namedParameters = namedParameters;
        this.returnType = null;
    }

    public void addNamedParameter(NamedParameter param) {
        namedParameters.add(param);
    }

    public NamedParameter addNamedParameter(String name, QName type, String mode) {
        NamedParameter param = new NamedParameter(name, type, mode);
        namedParameters.add(param);
        return param;
    }

    public NamedParameter addNamedParameter(String name, QName type, ParameterMode mode) {
        NamedParameter param = new NamedParameter(name, type, mode);
        namedParameters.add(param);
        return param;
    }

    public void removeNamedParameter(NamedParameter param) {
        namedParameters.remove(param);
    }

    public String getName() {
        return name;
    }

    public List getNamedParameters() {
        return namedParameters;
    }

    public QName getReturnType() {
        return returnType;
    }

    public void setReturnType(QName returnType) {
        this.returnType = returnType;
    }

    public Class getReturnClass() {
        return returnClass;
    }

    public void setReturnClass(Class returnClass) {
        this.returnClass = returnClass;
    }
}
