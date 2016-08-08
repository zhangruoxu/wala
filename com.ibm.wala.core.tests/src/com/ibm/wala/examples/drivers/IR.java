package com.ibm.wala.examples.drivers;

public class IR implements Comparable<IR> {
  public String methodSignature;
  public int lineNumber;

  public IR() {
  }

  public IR(String _methodSignature, int _lineNumber) {
    methodSignature = _methodSignature;
    lineNumber = _lineNumber;
  }

  @Override
  public String toString() {
    return methodSignature + ": " + lineNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IR) {
      IR ir = (IR) o;
      return methodSignature.equals(ir.methodSignature)
          && lineNumber == ir.lineNumber;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public int compareTo(IR ir) {
    if(!methodSignature.equals(ir.methodSignature)) {
      return methodSignature.compareTo(ir.methodSignature);
    } else {
      return new Integer(lineNumber).compareTo(ir.lineNumber);
    }
  }
}