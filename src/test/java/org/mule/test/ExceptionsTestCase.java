package org.mule.test;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.MuleException;
import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Messages;
import org.mule.config.i18n.Message;
import org.mule.impl.FailedToQueueEventException;
import org.mule.model.NoSatisfiableMethodsException;
import org.mule.model.TooManySatisfiableMethodsException;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.MessagingException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.endpoint.EndpointNotFoundException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ManagerException;
import org.mule.umo.model.ModelException;
import org.mule.umo.provider.NoReceiverForEndpointException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.CompressionException;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * @author Ross Mason
 */
public class ExceptionsTestCase extends TestCase
{
    public void testExceptionPrinting() {
        Exception e = new ManagerException(Message.createStaticMessage("Test Exception Message"),
                new MuleException(Message.createStaticMessage("Root Test Exception Message")));
        System.out.println(e.toString());
    }
    /*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
//    public void testExceptions() throws Exception
//    {
//        //Note for exceptions that added customer behaviour,
//        //additional tests for the special behaviour should be tested here
//        umoExceptionTest(MuleException.class);
//        umoExceptionTest(MuleRuntimeException.class);
//        umoExceptionTest(InitialisationException.class);
//        umoExceptionTest(DisposeException.class);
//        umoExceptionTest(ObjectNotFoundException.class);
//        umoExceptionTest(ContainerException.class);
//        umoExceptionTest(NoReceiverForEndpointException.class);
////        umoExceptionTest(CouldNotRouteInboundEventException.class);
////        umoExceptionTest(CouldNotRouteOutboundMessageException.class);
//        umoExceptionTest(EndpointNotFoundException.class);
//        umoExceptionTest(ModelException.class);
//        umoExceptionTest(MessagingException.class);
//        umoExceptionTest(FailedToQueueEventException.class);
//
//        umoExceptionTest(CompressionException.class);
//        umoExceptionTest(TransformerException.class);
//        umoExceptionTest(RoutePathNotFoundException.class);
//        umoExceptionTest(RoutingException.class);
//
//        umoExceptionTest(org.mule.transaction.IllegalTransactionStateException.class);
//        umoExceptionTest(org.mule.transaction.TransactionInProgressException.class);
//        umoExceptionTest(org.mule.transaction.TransactionNotInProgressException.class);
//        umoExceptionTest(org.mule.transaction.TransactionRollbackException.class);
//        umoExceptionTest(org.mule.transaction.TransactionStatusException.class);
//
//    }

//    public void testNoSatisfiableMethods()
//    {
//        Orange object = new Orange();
//        NoSatisfiableMethodsException exception =
//                new NoSatisfiableMethodsException(object, new IOException("something bad happened"));
//
//        assertNotNull(exception.getCause());
//        assertTrue(exception.getCause() instanceof IOException);
//        assertNotNull(exception.getMessage());
//
//        exception = new NoSatisfiableMethodsException(object);
//
//        assertNull(exception.getCause());
//        assertNotNull(exception.getMessage());
//    }

//    public void testUniqueIdNotSupported()
//    {
//        UMOMessageAdapter adapter = (UMOMessageAdapter) new Mock(UMOMessageAdapter.class).proxy();
//        UniqueIdNotSupportedException exception = new UniqueIdNotSupportedException(adapter);
//        exception = new UniqueIdNotSupportedException(adapter, "blah");
//        assertTrue(exception.getMessage().endsWith("blah"));
//        exception = new UniqueIdNotSupportedException(adapter, new Exception("foo"));
//        assertTrue(exception.getMessage().endsWith("foo"));
//
//    }
//
//    public void testTooManySatisfiableMethods()
//    {
//        Orange object = new Orange();
//        TooManySatisfiableMethodsException exception =
//                new TooManySatisfiableMethodsException(object, new IOException("something bad happened"));
//
//        assertNotNull(exception.getCause());
//        assertTrue(exception.getCause() instanceof IOException);
//        assertNotNull(exception.getMessage());
//
//        exception = new TooManySatisfiableMethodsException(object);
//
//        assertNull(exception.getCause());
//        assertNotNull(exception.getMessage());
//    }

//    protected void umoExceptionTest(Class exceptionClass) throws Exception
//    {
//        //assertTrue(UMOException.class.isAssignableFrom(exceptionClass));
//
//        Constructor ctor = exceptionClass.getConstructor(new Class[]{String.class, Throwable.class});
//        Throwable exception =
//                (Throwable) ctor.newInstance(new Object[]{"Something Failed", new IOException("Nested, something failed")});
//
//        assertNotNull(exception.getCause());
//        assertTrue(exception.getCause() instanceof IOException);
//        assertNotNull(exception.getMessage());
//
//        ctor = exceptionClass.getConstructor(new Class[]{String.class});
//        exception = (Throwable) ctor.newInstance(new Object[]{"Something Failed"});
//
//        assertNull(exception.getCause());
//        assertNotNull(exception.getMessage());
//    }
}
