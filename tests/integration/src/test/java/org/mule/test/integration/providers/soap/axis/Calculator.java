package org.mule.test.integration.providers.soap.axis;
 
public class Calculator implements CalculatorInterface {
  public long add(long i1, long i2)
  {
    return i1 + i2; 
  }


  public int subtract(int i1, int i2)
  {
    return i1 - i2;
  }
}
