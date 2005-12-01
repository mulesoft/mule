package jcej.httpInvoker;

public interface WorkInterface
{
    String executeByteArray(byte[] input);

    String executeString(String input);

    ComplexData executeComplexity(ComplexData input);
    
//    QueryResult executeQuery(RioQuery query);
}
