package reflection;

import java.lang.reflect.Method;

public class Reflect28 {
  public static void main(String[] args) {
    try {
      Class clzChild = Class.forName("reflection.B");
      Class clzBase = Class.forName("reflection.A");
      Object o = clzChild.newInstance();
      Method m = clzBase.getMethod("a");
      m.invoke(o);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
