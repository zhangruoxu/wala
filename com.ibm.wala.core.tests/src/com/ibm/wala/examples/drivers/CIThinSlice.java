package com.ibm.wala.examples.drivers;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
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
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

public class CIThinSlice {
  private static Set<IR> sliceStmts;
  private static Set<IR> sliceStmtsNolineNo;

  static {
    sliceStmts = new LinkedHashSet<IR>();
    sliceStmtsNolineNo = new LinkedHashSet<IR>();
  }
  
  public static void main(String[] args) throws Exception {    
//    System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(System.getProperty("user.home") + File.separator + "Research" + File.separator + "ECOOP16" + File.separator + "WALA" + File.separator + "CIWalaErr" + File.separator + "console.err")), true));
    System.out.println("******* CI Thin Slice begins at " + new Date());
    run(args);    
  }

  public static void run(String[] args) throws Exception {
    Properties p = CommandLine.parse(args);
    int calleeLineNumber = 0;
    String strCalleeLineNumber = p.getProperty("calleeLineNumber");
    if (strCalleeLineNumber != null) {
      calleeLineNumber = Integer.parseInt(p.getProperty("calleeLineNumber"));
    } else {
      System.out.println("Ignore line number");
    }

    run(p, p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"), calleeLineNumber);
  }

  public static Process run(Properties args, String appJar, String mainClass, String srcCaller, String srcCallee, int lineNumber) throws Exception {
    PrintWriter log = new PrintWriter(new File("log.txt"));
    Counter totalCounter = new Counter();
    totalCounter.begin();
    System.out.println("Run begins ...");
    
    File exclusionFile = (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    System.out.println("#### " + exclusionFile.getName());
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
    options.setReflectionOptions(ReflectionOptions.NO_FLOW_TO_CASTS);
    //for antlr
    //options.setReflectionOptions(ReflectionOptions.APPLICATION_GET_METHOD);    
    String refOption = options.getReflectionOptions().toString();
    System.out.println("Reflection option " + refOption);
    
    Counter cgCounter = new Counter();
    cgCounter.begin();
    //CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeVanillaZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope);
    System.out.println("Pointer analysis option: " + builder.getClass().getName());
    System.out.println("Make call graph......");
    CallGraph cg = builder.makeCallGraph(options, null);
    cgCounter.end();
    System.out.println("******* Call graph construction time " + cgCounter.getMinute() + " minutes, or " + cgCounter.getSecond() + " seconds.");
    
    System.out.println("Begin to find criteria......");
    Statement criterion = null;
   if (srcCallee != null) {
        System.out.println("Find call ......");
        criterion = SlicerTest.findCall(cg, srcCaller, srcCallee, lineNumber);
      } else {
        System.out.println("Find field load ......");
        criterion = SlicerTest.findFieldLoad(cg, srcCaller, args.getProperty("fieldSig"), lineNumber);
      }
    System.out.println("Statement: " + criterion);

    Counter sliceCounter = new Counter();
    sliceCounter.begin();
    ThinSlicer ts = new ThinSlicer(cg, builder.getPointerAnalysis());
    Collection<Statement> slice = ts.computeBackwardThinSlice(criterion);
    sliceCounter.end();
    totalCounter.end();
    System.out.println("******* Slice time " + sliceCounter.getMinute() + " minutes, or " + sliceCounter.getSecond() + " seconds.");
    System.out.println("******* Total time " + totalCounter.getMinute() + " minutes, or " + totalCounter.getSecond() + " seconds.");
    
    String root = System.getProperty("user.home") + File.separator + "walaOutput" + File.separator + "CIThin" + File.separator;
    //String root = ".." + File.separator + ".." + File.separator + "output" + File.separator + "CIThin" + File.separator;
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
    
    String sliceDump = root + mainClass.replace('/', '.') + "-" + callerName + "-" + target + "-" + lineNumber + "-" +  refOption + ".txt";
    SlicerTest.dumpSliceToFile(slice, sliceDump, criterion);
    
    String silceIRAllFileName = root + mainClass.replace('/', '.') + "-" + "all" + "-" + target + "-" + srcCallee + "-" + lineNumber + "-" + refOption + ".IR.txt";
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
      String stmtNoLineNo = root + mainClass.replace('/', '.') + "-" + "NoLineNo"  + "-" + callerName + "-" + target + "-" + lineNumber + "-" + refOption + ".txt";
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
  }
  
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
      return new IR(method.getSignature(), srcLineNumber);
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
}
