package jcej.httpInvoker;

import java.net.MalformedURLException;

import org.mule.umo.UMOException;
import org.mule.umo.manager.UMOManager;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class Client
{
    private static final String SPRING_ENDPOINT = "http://localhost:8003/springService";

    public static void main(String[] args) throws Exception
    {
        UMOManager manager = null;

        try
        {
            Client c = new Client();
            c.execute();
        }
        finally
        {
            if (manager != null) manager.dispose();
        }
    }

    private void execute() throws UMOException
    {
        invokeSpringService();
    }

    private void invokeSpringService()
    {
        ComplexData cd = new ComplexData("Foo", new Integer(13));
        HttpInvokerProxyFactoryBean invoker = new HttpInvokerProxyFactoryBean();
        invoker.setServiceInterface(WorkInterface.class);
        invoker.setServiceUrl(SPRING_ENDPOINT);
        try
        {
            invoker.afterPropertiesSet();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        WorkInterface worker = (WorkInterface) invoker.getObject();
        ComplexData data = worker.executeComplexity(cd);
        System.err.println(data);
    }
}
