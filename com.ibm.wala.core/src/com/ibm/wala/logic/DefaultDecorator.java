/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.logic.ILogicConstants.Quantifier;

public class DefaultDecorator implements ILogicDecorator {

  private final static DefaultDecorator INSTANCE = new DefaultDecorator();

  protected DefaultDecorator() {
  }

  public static DefaultDecorator instance() {
    return INSTANCE;
  }

  public String prettyPrint(BinaryConnective b) {
    return b.toString();
  }

  public String prettyPrint(BooleanConstant c) {
    return c.toString();
  }

  public String prettyPrint(Variable v) {
    return v.toString();
  }

  public String prettyPrint(Quantifier q) {
    return q.toString();
  }

  public String prettyPrint(IConstant constant) {
    return constant.toString();
  }

  public String prettyPrint(FunctionTerm term) {
    StringBuffer result = new StringBuffer(term.getFunction().getSymbol());
    result.append("(");
    for (int i = 0; i < term.getFunction().getNumberOfParameters() - 1; i++) {
      result.append(term.getParameters().get(i).prettyPrint(this));
      result.append(",");
    }
    if (term.getFunction().getNumberOfParameters() > 0) {
      result.append(term.getParameters().get(term.getFunction().getNumberOfParameters() - 1).prettyPrint(this));
    }
    result.append(")");
    return result.toString();
  }

}
