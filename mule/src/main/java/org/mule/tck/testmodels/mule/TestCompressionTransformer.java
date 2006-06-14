/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.transformers.compression.AbstractCompressionTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.GZipCompression;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TestCompressionTransformer extends AbstractCompressionTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1742674557192926869L;

    private String beanProperty1;
    private String containerProperty;

    private int beanProperty2;

    public TestCompressionTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return null;
    }

    public String getBeanProperty1()
    {
        return beanProperty1;
    }

    public void setBeanProperty1(String beanProperty1)
    {
        this.beanProperty1 = beanProperty1;
    }

    public int getBeanProperty2()
    {
        return beanProperty2;
    }

    public void setBeanProperty2(int beanProperty2)
    {
        this.beanProperty2 = beanProperty2;
    }

    public String getContainerProperty()
    {
        return containerProperty;
    }

    public void setContainerProperty(String containerProperty)
    {
        this.containerProperty = containerProperty;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone() ensures that isn't not cloned before all
     *      properties have been set on it
     */
    public Object clone() throws CloneNotSupportedException
    {
        if (containerProperty == null) {
            throw new IllegalStateException(
                    "Transformer cannot be cloned until all properties have been set on it");
        }
        return super.clone();
    }

}
