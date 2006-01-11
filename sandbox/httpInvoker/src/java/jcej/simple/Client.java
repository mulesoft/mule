package jcej.simple;

import java.net.MalformedURLException;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.manager.UMOManager;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor;

public class Client
{
    private static final String LOCAL_ENDPOINT = "vm://complexRequest";
    private static final String HTTP_ENDPOINT = "http://localhost:8000/httpService";
    private static final String AXIS_ENDPOINT = "axis:http://localhost:8002/axisService/doSomeWork";
    private static final String SPRING_ENDPOINT = "http://localhost:8003/springService";

    public static void main(String[] args) throws Exception
    {
        UMOManager manager = null;

        try
        {
            MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
            manager = builder.configure("simple/conf/client-mule-config.xml");

            Client c = new Client();
            c.execute();
        }
        finally
        {
            if (manager != null) manager.dispose();
        }
    }

    private MuleClient client;

    private void execute() throws UMOException
    {
        client = new MuleClient();

        try
        {
            invokeSpringService();
//            invokeHttpService();
//            executeString();
//            executeComplexity();
//            complexRequest();
        }
        finally
        {
            if (client != null) client.dispose();
        }
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

    /*
    private void invokeHttpService() throws UMOException
    {
        System.err.println("\ninvokeHttpService");
        Object result = client.send(HTTP_ENDPOINT, "Hello Http Service", null);
        System.err.println(result);
        UMOMessage message = (UMOMessage) result;
        System.err.println(message.getPayload());
    }

    private void executeString() throws UMOException
    {
        System.err.println("\nexecuteString");
        Object result = client.send(AXIS_ENDPOINT + "?method=executeString",
                "Hello Axis Service", null);
        System.err.println(result);
        UMOMessage message = (UMOMessage) result;
        System.err.println(message.getPayload());
    }
    
    private void executeComplexity() throws UMOException
    {
        System.err.println("\nexecuteComplexity");
        Object result = client.send(AXIS_ENDPOINT + "?method=executeComplexity",
                new ComplexData("Foo", new Integer(42)), null);
        System.err.println(result);
        UMOMessage message = (UMOMessage) result;
        ComplexData data = (ComplexData) message.getPayload();
        System.err.println(data);
    }

    private void complexRequest() throws UMOException
    {
        System.err.println("\ncomplexRequest");
        Object result = client.send(LOCAL_ENDPOINT, new ComplexData("Foo", new Integer(84)), null);
        System.err.println(result);
        UMOMessage message = (UMOMessage) result;
        ComplexData data = (ComplexData) message.getPayload();
        System.err.println(data);
    }
    */
}
