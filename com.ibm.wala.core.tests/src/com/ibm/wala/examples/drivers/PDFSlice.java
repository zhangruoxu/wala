/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.drivers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

/**
 * 
 * This simple example WALA application computes a slice (see {@link Slicer})
 * and fires off the PDF viewer to view a dot-ted representation of the slice.
 * 
 * This is an example program on how to use the slicer.
 * 
 * See the 'PDFSlice' launcher included in the 'launchers' directory.
 * 
 * @see Slicer
 * @author sfink
 */
public class PDFSlice {

  /**
   * Usage: PDFSlice -appJar [jar file name] -mainClass [main class] -srcCaller
   * [method name] -srcCallee [method name] -dd [data dependence options] -cd
   * [control dependence options] -dir [forward|backward]
   * 
   * <ul>
   * <li>"jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * <li>"main class" should beshould be something like
   * "c:/temp/testdata/java_cup.jar"
   * <li>"method name" should be the name of a method. This takes a slice from
   * the statement that calls "srcCallee" from "srcCaller"
   * <li>"data dependence options" can be one of "-full", "-no_base_ptrs",
   * "-no_base_no_heap", "-no_heap", "-no_base_no_heap_no_cast", or "-none".
   * </ul>
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   * 
   * @see com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions <li>
   *      "control dependence options" can be "-full" or "-none" <li>the -dir
   *      argument tells whether to compute a forwards or backwards slice. </ul>
   * 
   */
  private static Set<IR> sliceStmts;
  private static Set<IR> sliceStmtsNolineNo;

  static {
    sliceStmts = new LinkedHashSet<IR>();
    sliceStmtsNolineNo = new LinkedHashSet<IR>();
  }

  public PDFSlice() {}

  public static void main(String[] args) throws WalaException, IllegalArgumentException, CancelException, IOException {
    //System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(System.getProperty("user.home") + File.separator + 
    //    "WalaErr" + File.separator + "console.err")), true));
    System.out.println("******* " + new Date());
    PDFSlice.run(args);
  }

  /**
   * see {@link #main(String[])} for command-line arguments
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static void run(String[] args) throws WalaException, IllegalArgumentException, CancelException, IOException {
    // parse the command-line into a Properties object
    Properties p = CommandLine.parse(args);
    // validate that the command-line has the expected format
    validateCommandLine(p);
    // 0 indicates ignore line number
    int calleeLineNumber = 0;
    String strCalleeLineNumber = p.getProperty("calleeLineNumber");
    if (strCalleeLineNumber != null) {
      calleeLineNumber = Integer.parseInt(p.getProperty("calleeLineNumber"));
    } else {
      System.err.println("ine number is ignored.");
    }

    // common slicing
    /*
     * run(p.getProperty("appJar"), p.getProperty("mainClass"),
     * p.getProperty("srcCaller"), p.getProperty("srcCallee"), calleeLineNumber,
     * goBackward(p), DataDependenceOptions.NO_HEAP,
     * ControlDependenceOptions.FULL);
     */
    // CS thin slicing
    run(p, p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"),
        calleeLineNumber, goBackward(p), DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
  }

  /**
   * Should the slice be a backwards slice?
   */
  private static boolean goBackward(Properties p) {
    return !p.getProperty("dir", "backward").equals("forward");
  }

  public static Process run(Properties args, String appJar, String mainClass, String srcCaller, String srcCallee, int lineNumber,
      boolean goBackward, DataDependenceOptions dOptions, ControlDependenceOptions cOptions) throws IllegalArgumentException,
      CancelException, IOException {
    Counter totalCounter = new Counter();
    totalCounter.begin();
    try {
      System.out.println("Run begins ...");
      // create an analysis scope representing the appJar as a J2SE application
      File exclusionFile = (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile);

      System.out.println("These libraries are excluded > ");
      BufferedReader reader = new BufferedReader(new FileReader(exclusionFile));
      String temp = null;
      while((temp = reader.readLine()) != null) {
        System.out.println(temp);
      }
      reader.close();

      System.out.println("Build class hierarchy......");

      Counter chaCounter = new Counter();
      chaCounter.begin();
      ClassHierarchy cha = ClassHierarchy.make(scope);
      chaCounter.end();

      System.out.println("******* CHA time " + chaCounter.getMinute() + " minutes, or " + chaCounter.getSecond() + " seconds.");

      Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
      AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
      // Reflection option can be modified here
      options.setReflectionOptions(ReflectionOptions.NO_FLOW_TO_CASTS);
      //for antlr
      //options.setReflectionOptions(ReflectionOptions.APPLICATION_GET_METHOD);
      String refOption = options.getReflectionOptions().toString();
      System.out.println("Reflection option " + refOption);

      Counter cgCounter = new Counter();
      cgCounter.begin();
      // Pointer analysis can be modified here
      //CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
      //CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, new AnalysisCache(), cha, scope);
      // CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      //CallGraphBuilder builder = Util.makeVanillaZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope);

      CallGraphBuilder builder = null;
      String pta = args.getProperty("pta", "vanillaZeroOneCFA");
      if (pta.equals("vanillaZeroOneCFA")) {
        builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      } else if (pta.equals("zeroOneCFA")) {
        builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      } else {
        builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      }
      System.out.println("Pointer analysis option: " + builder.getClass().getName());
      System.out.println("Make call graph......");
      CallGraph cg = builder.makeCallGraph(options, null);
      cgCounter.end();
      System.out.println("******* Call graph construction time " + cgCounter.getMinute() + " minutes, or " + cgCounter.getSecond() + " seconds.");

      //SlicerTest.printCG(cg);

      System.out.println("Begin to find criteria......");
      Statement criterion = null;
      if (srcCallee != null) {
        System.out.println("Find call ......");
        criterion = SlicerTest.findCall(cg, srcCaller, srcCallee, lineNumber);
      } else {
        System.out.println("Find field load ......");
        criterion = SlicerTest.findFieldLoad(cg, srcCaller, args.getProperty("fieldSig"), lineNumber);
      }
      System.out.println("Statement: " + criterion.toString());

      Counter sliceCounter = new Counter();
      sliceCounter.begin();
      Collection<Statement> slice = null;
      if (goBackward) {
        System.out.println("Begin to slice......");

        slice = Slicer.computeBackwardSlice(criterion, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      } else {
        criterion = getReturnStatementForCall(criterion);
        slice = Slicer.computeForwardSlice(criterion, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      }
      sliceCounter.end();
      totalCounter.end();
      System.out.println("******* Slice time " + sliceCounter.getMinute() + " minutes, or " + sliceCounter.getSecond() + " seconds.");
      System.out.println("******* Total time " + totalCounter.getMinute() + " minutes, or " + totalCounter.getSecond() + " seconds.");

      String root = System.getProperty("user.home") + File.separator + "walaOutput" + File.separator;
      //String root = ".." + File.separator + ".." + File.separator + "output" + File.separator;
      String bm = args.getProperty("bm");
      if(bm != null) {
        root += bm + File.separator;
      } else {
        System.out.println("Benchmark name is not specified. Use " + root  + " output directory.");
      }

      File rootFile = new File(root);
      if(!rootFile.exists()) {
        rootFile.mkdirs();
      }

      String[] callerInfo = srcCaller.split("\\.");
      String callerName = callerInfo[1];
      String target = null;
      if(srcCallee != null) {
        target = srcCallee;
      } else {
        String[] fieldInfo = args.getProperty("fieldSig").split(":");
        target = fieldInfo[0].replace("/", ".");
      }
      String sliceDump = root + mainClass.replace('/', '.') + "-" + callerName + "-" + target + "-" + lineNumber + "-" + dOptions + "-" + cOptions + "-" + refOption + ".txt";
      SlicerTest.dumpSliceToFile(slice, sliceDump, criterion);
      System.out.println(sliceDump);

      String silceIRAllFileName = root + mainClass.replace('/', '.') + "-" + "all" + "-" + target + "-" + srcCallee + "-" + lineNumber + "-" + dOptions + "-" + cOptions + "-" + refOption + "-IR.txt";
      File silceIRAll = new File(silceIRAllFileName);
      System.out.println(silceIRAllFileName);
      PrintWriter writerAll = new PrintWriter(silceIRAll);

      for (Statement stmt : slice) {
        IR ir = dumpStmtToFile(stmt);
        if(ir.lineNumber == -1) {
          sliceStmtsNolineNo.add(ir);
        } else {
          sliceStmts.add(ir);
        }
      }

      for(IR ir : sliceStmts) {
        writerAll.println(ir.methodSignature + " {" + ir.lineNumber + "}");
        System.out.println(ir.methodSignature + " {" + ir.lineNumber + "}");
      }

      writerAll.close();

      if(!sliceStmtsNolineNo.isEmpty()) {
        String stmtNoLineNo = root + mainClass.replace('/', '.') + "-" + "NoLineNo"  + "-" + callerName + "-" + target + "-" + lineNumber + "-" + dOptions + "-" + cOptions + "-" + refOption + ".txt";
        File stmtNoLineNoFile = new File(stmtNoLineNo);
        PrintWriter writerNoLineNo = new PrintWriter(stmtNoLineNoFile);
        for (IR m : sliceStmtsNolineNo) {
          writerNoLineNo.println(m);
        }
        writerNoLineNo.close();
      }

      System.out.println("The number of statements with line number " + sliceStmts.size());
      System.out.println("The number of statements without line number " + sliceStmtsNolineNo.size());
      return null;
    } catch (WalaException e) {
      e.printStackTrace();
      return null;
    }
  }

  /*
   * get line number in source code.
   * Return: the statement does not have corresponding line number
   */
  public static IR dumpStmtToFile(Statement stmt) {
    int srcLineNumber = -1;
    // fetch line number for common statements
    IMethod method = stmt.getNode().getMethod();
    if (stmt instanceof StatementWithInstructionIndex) {
      int instIndex = ((StatementWithInstructionIndex) stmt).getInstructionIndex();
      int bcIndex = 0;
      ShrikeBTMethod btMethod = null;

      if (method instanceof ShrikeBTMethod) {
        btMethod = (ShrikeBTMethod) method;
      } else {
        System.err.println("+++++++ No line number. Method is " + method);
        System.err.println("+++++++ No line number. Stmt is " + stmt);
        return new IR(method.getSignature(), -1);
      }
      try {
        bcIndex = btMethod.getBytecodeIndex(instIndex);
      } catch (InvalidClassFileException e) {
        System.err.println("+++++++ Exception :" + method);
        return new IR(btMethod.getSignature(), -1);
      } catch (ArrayIndexOutOfBoundsException aioobe) {
        System.err.println("+++++++ Exception :" + method);
        return new IR(btMethod.getSignature(), -1);
      }
      srcLineNumber = stmt.getNode().getMethod().getLineNumber(bcIndex);
      System.out.println(stmt);
      IR ir = new IR(method.getSignature(), srcLineNumber);
      System.out.println(ir);
      return ir;
    }
    // fetch line number for catch statement
    if (stmt instanceof GetCaughtExceptionStatement) {
      GetCaughtExceptionStatement catchStmt = (GetCaughtExceptionStatement) stmt;
      ISSABasicBlock bb = catchStmt.getNode().getIR().getBasicBlockForCatch(catchStmt.getInstruction());
      IBytecodeMethod bytecodeMethod = null;

      if (method instanceof IBytecodeMethod) {
        bytecodeMethod = (IBytecodeMethod) method;
      } else {
        return new IR(method.getSignature(), -1);
      }
      try {
        int bcIndex = bytecodeMethod.getBytecodeIndex(bb.getFirstInstructionIndex());
        srcLineNumber = bytecodeMethod.getLineNumber(bcIndex);
        return new IR(method.getSignature(), srcLineNumber);
      } catch (InvalidClassFileException e) {
        return new IR(method.getSignature(), -1);
      }
    }
    System.err.println("+++++++Not the statements with line number. " + stmt);
    return new IR(method.getSignature(), -1);
  }

  public static Statement getReturnStatementForCall(Statement s) {
    if (s.getKind() == Kind.NORMAL) {
      NormalStatement n = (NormalStatement) s;
      SSAInstruction st = n.getInstruction();
      if (st instanceof SSAInvokeInstruction) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) st;
        if (call.getCallSite().getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
          throw new IllegalArgumentException("this driver computes forward slices from the return value of calls.\n" + ""
              + "Method " + call.getCallSite().getDeclaredTarget().getSignature() + " returns void.");
        }
        return new NormalReturnCaller(s.getNode(), n.getInstructionIndex());
      } else {
        return s;
      }
    } else {
      return s;
    }
  }

  static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
    if (p.get("mainClass") == null) {
      throw new UnsupportedOperationException("expected command-line to include -mainClass");
    }
    if (p.get("srcCallee") == null && p.get("fieldSig") == null) {
      throw new UnsupportedOperationException("expected command-line to include -srcCallee or -fieldSig");
    }
    if (p.get("srcCaller") == null) {
      throw new UnsupportedOperationException("expected command-line to include -srcCaller");
    }
  }
}
