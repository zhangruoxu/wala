/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.shrike.cg;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;

public class Runtime {
  public interface Policy {
    void callback(StackTraceElement[] stack, String klass, String method, Object receiver);
  }
  
  private static class DefaultCallbackPolicy implements Policy {
    // if not found:
    //  look up the stack for expected caller
    //  if found:
    //    (callback case)
    //    record real target of expected caller (or not)
    //    policy-based edge for call to current method
    //  if not found:
    //    (async system edge)
    //    policy-based edge for call to current method
   @Override
    public void callback(StackTraceElement[] stack, String klass, String method, Object receiver) {
     // stack frames: Runtime.execution(0), callee(1), caller(2)
     String root = "<clinit>".equals(stack[1].getMethodName())? "clinit": "callbacks";
     String line = root + "\t" + bashToDescriptor(klass) + "\t" + String.valueOf(method) + "\n";
     synchronized (runtime) {
       if (runtime.output != null) {
         runtime.output.printf(line);
         runtime.output.flush();
       }
     }
   }
  }
  
  private static final Runtime runtime = 
      new Runtime(System.getProperty("dynamicCGFile"), 
                  System.getProperty("dynamicCGFilter"),
                  System.getProperty("policyClass", "com.ibm.wala.shrike.cg.Runtime$DefaultPolicy"));
  
  private PrintWriter output;
  private SetOfClasses filter;
  private Policy handleCallback;
  
  private ThreadLocal<Stack<String>> callStacks = new ThreadLocal<Stack<String>>() {

    @Override
    protected Stack<String> initialValue() {
      Stack<String> callStack = new Stack<String>();
      callStack.push("root");
      return callStack;
    }
 
  };
  
  private Runtime(String fileName, String filterFileName, String policyClassName) {
    try {
      filter = new FileOfClasses(new FileInputStream(filterFileName));
    } catch (Exception e) {
      filter = null;
    }

    try {
      output = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }
    
    try {
      handleCallback = (Policy) Class.forName(policyClassName).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      handleCallback = new DefaultCallbackPolicy();
    }
    
    java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        endTrace();
      }
    });
  }

  public static void endTrace() {
    synchronized (runtime) {
      if (runtime.output != null) {
        runtime.output.close();
        runtime.output = null;
      }
    }
  }
  
  public static Object NULL_TAG = new Object() {
    @Override
    public String toString() {
      return "NULL TAG";
    }
  };
  
  public static String bashToDescriptor(String className) {
    if (className.startsWith("class ")) {
      className = className.substring(6);
    }
    if (className.indexOf('.') >= 0) {
      className = className.replace('.', '/');
    }
    return className;
  }
  
  public static void execution(String klass, String method, Object receiver) {
    if (runtime.filter == null || ! runtime.filter.contains(bashToDescriptor(klass))) {
      if (runtime.output != null) {
        String caller = runtime.callStacks.get().peek();
        
        checkValid: {
          //
          // check for expected caller
          //
          if (runtime.handleCallback != null) {
            StackTraceElement[] stack = (new Throwable()).getStackTrace();
            if (stack.length > 2) {
              // frames: Runtime.execution(0), callee(1), caller(2)
              StackTraceElement callerFrame = stack[2];
              if (! caller.contains(callerFrame.getMethodName()) ||
                  ! caller.contains(bashToDescriptor(callerFrame.getClassName()))) {
                runtime.handleCallback.callback(stack, klass, method, receiver);
                break checkValid;
              }
            }
          }
        
          String line = String.valueOf(caller) + "\t" + bashToDescriptor(klass) + "\t" + String.valueOf(method) + "\n";
          synchronized (runtime) {
            if (runtime.output != null) {
              runtime.output.printf(line);
              runtime.output.flush();
            }
          }
        }
      }
    }

    runtime.callStacks.get().push(bashToDescriptor(klass) + "\t" + method);
  }
  
  public static void termination(String klass, String method, Object receiver, boolean exception) {
    runtime.callStacks.get().pop();
  }
  
  public static void pop(String klass, String method) {
 
  }
  
  public static void addToCallStack(String klass, String method, Object receiver) {

  }
}
