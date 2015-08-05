package com.ibm.wala.examples.drivers;


import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

public class PDFThinSlice {
  public static void main(String[] args) throws Exception {
    Date start = new Date();
    run(args);    
    Date end = new Date();

    long diff = end.getTime() - start.getTime();
    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
    System.out.println("### Has run for " + seconds + " seconds or " + minutes + " minutes ###");
  }

  public static void run(String[] args) throws Exception {
    Properties p = CommandLine.parse(args);
    int calleeLineNumber = 0;
    String strCalleeLineNumber = p.getProperty("calleeLineNumber");
    if (strCalleeLineNumber != null) {
      calleeLineNumber = Integer.parseInt(p.getProperty("calleeLineNumber"));
    } else {
      System.err.println("Ignore line number");
    }

    run(p.getProperty("bm"), p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"), calleeLineNumber);
  }

  public static void run(String bm, String appJar, String mainClass, String srcCaller, String srcCallee, int calleeLineNumber) throws Exception {
    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    options.setReflectionOptions(ReflectionOptions.NO_FLOW_TO_CASTS);
    //CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeVanillaZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    Statement calleeStmt = SlicerTest.findCallee(cg, srcCaller, srcCallee, calleeLineNumber);

    System.err.println("####### Statement " + calleeStmt);

    ThinSlicer ts = new ThinSlicer(cg, builder.getPointerAnalysis());
    Collection<Statement> slice = ts.computeBackwardThinSlice(calleeStmt);
    String root = System.getProperty("user.home") + File.separator + "walaOutput" + File.separator + "CIThin" + File.separator;
    if(bm != null) {
      root += bm + File.separator;
    } else {
      System.err.println("Benchmark name is not specified. Use " + root  + "output directory.");
    }
    
    File rootFile = new File(root);
    if(!rootFile.exists()) {
      rootFile.mkdirs();
    }
    
    String sliceStmts = root + mainClass.replace('/', '.') + "-" + srcCaller + "-" + srcCallee + "-" + calleeLineNumber + ".txt";
    SlicerTest.dumpSliceToFile(slice, sliceStmts, calleeStmt);
    System.err.println(sliceStmts);
    
    String sliceIRAppFileName = root + mainClass.replace('/', '.') + "-" + "app" + "-" + srcCaller + "-" + srcCallee + "-" + calleeLineNumber + ".IR.txt";
    File sliceIRApp = new File(sliceIRAppFileName);
    System.err.println(sliceIRAppFileName);
    PrintWriter writerApp = new PrintWriter(sliceIRApp);

    String silceIRAllFileName = root + mainClass.replace('/', '.') + "-" + "all" + "-" + srcCaller + "-" + srcCallee + "-" + calleeLineNumber + ".IR.txt";
    File silceIRAll = new File(silceIRAllFileName);
    System.err.println(silceIRAllFileName);
    PrintWriter writerAll = new PrintWriter(silceIRAll);
    
    // this container is used to save the statements without line number
    Vector<String> mtdWithNoLineNo = new Vector<String>();

    // here fetch line number for each statement
    for (Statement stmt : slice) {
      // Primordial indicates library code? 
      if (stmt.getNode().getMethod().toString().contains("Primordial")) {
        Statement s = dumpStmtToFile(stmt, writerAll);
        if (s != null) {
          mtdWithNoLineNo.add(s.toString());
        }
        continue;
      } else {
        Statement s = dumpStmtToFile(stmt, writerAll);
        dumpStmtToFile(stmt, writerApp);
        if (s != null) {
          mtdWithNoLineNo.add(s.toString());
        }
      }
    }
    writerAll.close();
    writerApp.close();

    Collections.sort(mtdWithNoLineNo);
    System.err.println("###### Statements without line number: ");
    for (String m : mtdWithNoLineNo) {
      System.out.println(m);
    }
  }
  
  public static Statement dumpStmtToFile(Statement stmt, PrintWriter writer) {
    // fetch line number for common statements
    IMethod method = stmt.getNode().getMethod();
    if (stmt instanceof StatementWithInstructionIndex) {
      int instIndex = ((StatementWithInstructionIndex) stmt).getInstructionIndex();
      int bcIndex = 0;
      ShrikeBTMethod btMethod = null;

      if (method instanceof ShrikeBTMethod) {
        btMethod = (ShrikeBTMethod) method;
      } else {
        System.err.println("Is not ShrikeBTMethod " + method);
        return stmt;
      }
      try {
        bcIndex = btMethod.getBytecodeIndex(instIndex);
      } catch (InvalidClassFileException e) {
        System.err.println("cannot fetch line number for " + method);
        return stmt;
      } catch (ArrayIndexOutOfBoundsException aioobe) {
        System.err.println("Bytecode index out of bound");
        System.err.println("Method " + method);
        return stmt;
      }
      int srcLineNumber = stmt.getNode().getMethod().getLineNumber(bcIndex);
      writer.println(method.getSignature() + " {" + srcLineNumber + "}");
      System.out.println(((StatementWithInstructionIndex) stmt).getInstruction() + " {" + srcLineNumber + "}");
      System.out.println(method.getSignature() + " {" + srcLineNumber + "}");
      return null;
    }
    // fetch line number for catch statement
    if (stmt instanceof GetCaughtExceptionStatement) {
      GetCaughtExceptionStatement catchStmt = (GetCaughtExceptionStatement) stmt;
      ISSABasicBlock bb = catchStmt.getNode().getIR().getBasicBlockForCatch(catchStmt.getInstruction());
      IBytecodeMethod bytecodeMethod = null;

      if (method instanceof IBytecodeMethod) {
        bytecodeMethod = (IBytecodeMethod) method;
      } else {
        System.err.println("Is not IBytecodeMethod " + method);
        return stmt;
      }

      try {
        int bcIndex = bytecodeMethod.getBytecodeIndex(bb.getFirstInstructionIndex());
        int lineNumber = bytecodeMethod.getLineNumber(bcIndex);
        writer.println(bytecodeMethod.getSignature() + " {" + lineNumber + "}");
        System.out.println("catch statement line number " + bytecodeMethod.getSignature() + " {" + lineNumber + "}");
        return null;
      } catch (InvalidClassFileException e) {
        System.err.println("cannot fetch line number for " + bytecodeMethod);
        return stmt;
      }
    }
    return stmt;
  }
}
