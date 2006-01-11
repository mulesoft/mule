package org.mule.providers.spring;

import org.mule.impl.UMODescriptorAware;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.ClassHelper;
import org.mule.util.Utility;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SpringHttpInvokerComponent implements UMODescriptorAware, Callable
{
    private Delegate delegate;

    private class Delegate extends RemoteInvocationBasedExporter implements InitializingBean
    {
        private Object proxy;

        public void afterPropertiesSet()
        {
            this.proxy = getProxyForService();
        }

        public Object execute(RemoteInvocation invocation)
        {
            try {
                Object value = invoke(invocation, proxy);
                return value;
            }
            catch (Throwable ex) {
                ex.printStackTrace();
                return new RemoteInvocationResult(ex);
            }
        }
    }

    public SpringHttpInvokerComponent()
    {
        delegate = new Delegate();
    }

    public void setDescriptor(UMODescriptor descriptor)
    {
        Map properties = descriptor.getProperties();
        try
        {
            setPojo(properties);
            org.mule.util.BeanUtils.populateWithoutFail(delegate, properties, true);
            delegate.afterPropertiesSet();
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to create/configure RemoteInvocationBasedExporter", e);
        }
    }

    private void setPojo(Map properties) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        // Instantiate a POJO named by serviceClass
        String serviceClass = (String) properties.remove("serviceClass");

        if(serviceClass!=null && !serviceClass.equals(Utility.EMPTY_STRING))
        {
            Object service = ClassHelper.instanciateClass(serviceClass, null);
            delegate.setService(service);
        }
        // Alternative:
        // Locate a Spring bean named by serviceBean
        String serviceBean = (String) properties.remove("serviceBean");
        if(serviceBean!=null && !serviceBean.equals(Utility.EMPTY_STRING))
        {
            // How do I find the Spring bean named by 'serviceBean'?
            Object service = null;
            delegate.setService(service);
        }
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        Object transformedMessage = eventContext.getTransformedMessage();
        RemoteInvocation ri = (RemoteInvocation) transformedMessage;
        Object rval = delegate.execute(ri);
        return rval;
    }
}
