package reflection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;

public class Reflect29 {
  public static void main(String[] args) {
    String clzName = read("C:\\Users\\yifei\\Desktop\\PLDI'16\\benchmarks\\test\\mtdName.txt");
    String mtdName = read("C:\\Users\\yifei\\Desktop\\PLDI'16\\benchmarks\\test\\mtdName.txt");
    /*try {
      Class<?> clz = Class.forName("reflection.C");
      Object o = clz.newInstance();
      Method m = clz.getMethod(mtdName);
      m.invoke(o, blah());
    } catch (Exception e) {
      e.printStackTrace();
    }*/
    
    try {
      Class<?> clz = Class.forName(clzName);
      C c = (C)clz.newInstance();
      /*Method m = clz.getMethod(mtdName);
      m.invoke(c, 1);*/
      c.foo();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  private static String read(String fileName) {
    String content = null;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
      content = reader.readLine();
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return content;
  }
  
  private static Object[] blah() {
    return null;
  }
}
