package reflection;

public class A {
  public A() {
    System.out.println("A.<init> is invoked.");
    a();
  }
  
  public void a() {
    System.out.println("A.a()");
  }
  public void a(int a) {
    System.out.println("A.a(I)");
  }
   
  public void b() {
    System.out.println("A.b()");
  }
  
  public void b(int b) {
    System.out.println("A.b(I)");
  }
  
  public void a(String o) {
    System.out.println("A.a(Ljava/lang/String;)");
  }
  
  public void b(String o) {
    System.out.println("A.b(Ljava/lang/String;)");
  }
  public static void x() {
    System.out.println("A.x()");
  }
  public static void y() {
    System.out.println("A.y()");
  }
}

