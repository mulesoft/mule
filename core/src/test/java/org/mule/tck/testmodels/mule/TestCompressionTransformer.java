/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.compression.AbstractCompressionTransformer;
import org.mule.util.compression.GZipCompression;

public class TestCompressionTransformer extends AbstractCompressionTransformer
{

    private String beanProperty1;
    private String containerProperty;

    private int beanProperty2;

    public TestCompressionTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
    }

    @Override
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

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        if (containerProperty == null)
        {
            throw new IllegalStateException(
                "Transformer cannot be cloned until all properties have been set on it");
        }

        return super.clone();
    }

}
