package com.ibm.wala.examples.drivers;

public class Test {
  public static void main(String[] args) {
    try {
      Class clz = Class.forName("com.ibm.wala.examples.drivers.A");
      Object o = clz.newInstance();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
