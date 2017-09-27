/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.kotlin.evaluator;

import com.twosigma.beakerx.evaluator.InternalVariable;
import com.twosigma.beakerx.jvm.object.SimpleEvaluationObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.twosigma.beakerx.evaluator.BaseEvaluator.INTERUPTED_MSG;

class KotlinCodeRunner<T> implements Runnable {

  private final SimpleEvaluationObject theOutput;
  private final T instance;
  private final Method theMth;
  private final ClassLoader loader;

  public KotlinCodeRunner(T instance, Method mth, SimpleEvaluationObject out, ClassLoader ld) {
    this.instance = instance;
    theMth = mth;
    theOutput = checkNotNull(out);
    loader = ld;
  }

  @Override
  public void run() {
    ClassLoader oldld = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(loader);
    theOutput.setOutputHandler();
    InternalVariable.setValue(theOutput);
    try {
      InternalVariable.setValue(theOutput);
      Object o = theMth.invoke(instance, (Object[]) null);
      theOutput.finished(calculateResult(o));
    } catch (Throwable e) {
      if (e instanceof InvocationTargetException)
        e = ((InvocationTargetException) e).getTargetException();
      if ((e instanceof InterruptedException) || (e instanceof ThreadDeath)) {
        theOutput.error(INTERUPTED_MSG);
      } else {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        theOutput.error(sw.toString());
      }
    } finally {
      theOutput.executeCodeCallback();
    }
    theOutput.clrOutputHandler();
    Thread.currentThread().setContextClassLoader(oldld);
  }

  private Object calculateResult(Object o) {
    if (o != null && o.toString().equals("kotlin.Unit")) {
      return null;
    }
    return o;
  }
}
