package reflection;

import java.lang.reflect.Method;

public class Reflect24 {
  public static void main(String[] args) {
    try {
      Class<?> clz = Class.forName("reflection.A");
      Object o = clz.newInstance();
      Method ma = clz.getMethod("a");
      ma.invoke(o, 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
}
