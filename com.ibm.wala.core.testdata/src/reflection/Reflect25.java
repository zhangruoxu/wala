package reflection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;

public class Reflect25 {
  public static void main(String[] args) {
    //String clzName = read("C:\\Users\\yifei\\Desktop\\PLDI'16\\benchmarks\\test\\clzName.txt");
    String methodName = read("C:\\Users\\yifei\\Desktop\\PLDI'16\\benchmarks\\test\\mtdName.txt");
    try {
      Class<?> clz = Class.forName("reflection.A");
      Object o = clz.newInstance();
      Method m = clz.getMethod(methodName, String.class);
      m.invoke(o, "x");
    } catch (Exception e) {
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
}
