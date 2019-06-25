/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2018 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.sensors.functioncomplexity;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import java.util.Hashtable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.api.CxxMetric;
import org.sonar.cxx.parser.CxxGrammarImpl;
import org.sonar.cxx.sensors.squid.SquidSensor;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;
import org.sonar.squidbridge.checks.ChecksHelper;

public class CxxFunctionComplexitySquidSensor extends SquidAstVisitor<Grammar> implements SquidSensor {

  private static final Logger LOG = Loggers.get(CxxFunctionComplexitySquidSensor.class);

  public static final String FUNCTION_COMPLEXITY_THRESHOLD_KEY = "funccomplexity.threshold";

  private int cyclomaticComplexityThreshold;

  private int functionsBelowThreshold;

  private int functionsOverThreshold;

  private int linesOfCodeBelowThreshold;

  private int linesOfCodeOverThreshold;

  private Hashtable<SourceFile, FunctionCount> complexFunctionsPerFile = new Hashtable<>();

  private Hashtable<SourceFile, FunctionCount> locInComplexFunctionsPerFile = new Hashtable<>();

  private String fileName;

  public CxxFunctionComplexitySquidSensor(CxxLanguage language){
    this.cyclomaticComplexityThreshold = language.getIntegerOption(FUNCTION_COMPLEXITY_THRESHOLD_KEY).orElse(10);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Cyclomatic complexity threshold: " + this.cyclomaticComplexityThreshold);
    }
  }

  @Override
  public SquidAstVisitor<Grammar> getVisitor() {
    return this;
  }

  @Override
  public void init() {
    subscribeTo(CxxGrammarImpl.functionBody);
  }

  @Override
  public void leaveNode(AstNode node) {
      SourceFunction sourceFunction = (SourceFunction) getContext().peekSourceCode();
      SourceFile sourceFile = sourceFunction.getAncestor(SourceFile.class);

      int complexity = ChecksHelper.getRecursiveMeasureInt(sourceFunction, CxxMetric.COMPLEXITY);
      int lineCount = sourceFunction.getInt(CxxMetric.LINES_OF_CODE_IN_FUNCTION_BODY);

      incrementFunctionByThresholdForAllFiles(complexity, lineCount);
      incrementFunctionByThresholdForFile(sourceFile, complexity, lineCount);
  }

  private void incrementFunctionByThresholdForAllFiles(int complexity, int lineCount){
      if (complexity > this.cyclomaticComplexityThreshold){
          this.functionsOverThreshold++;
          this.linesOfCodeOverThreshold += lineCount;
      }
      else {
          this.functionsBelowThreshold++;
          this.linesOfCodeBelowThreshold += lineCount;
      }
  }

  private void incrementFunctionByThresholdForFile(SourceFile sourceFile, int complexity, int loc){
    if (!complexFunctionsPerFile.containsKey(sourceFile)) {
      complexFunctionsPerFile.put(sourceFile, new FunctionCount());
    }

    if (!locInComplexFunctionsPerFile.containsKey(sourceFile)) {
      locInComplexFunctionsPerFile.put(sourceFile, new FunctionCount());
    }

    FunctionCount functionCount = complexFunctionsPerFile.get(sourceFile);
    FunctionCount locCount = locInComplexFunctionsPerFile.get(sourceFile);
    if (complexity > this.cyclomaticComplexityThreshold){
        functionCount.countOverThreshold++;
        locCount.countOverThreshold += loc;
    }
    else {
        functionCount.countBelowThreshold++;
        locCount.countBelowThreshold += loc;
    }
  }

  @Override
  public void publishMeasureForFile(InputFile inputFile, SourceFile squidFile, SensorContext context) {
    publishComplexFunctionMetricsForFile(inputFile, squidFile, context);
    publishLocInComplexFunctionMetricsForFile(inputFile, squidFile, context);
  }

  private void publishComplexFunctionMetricsForFile(InputFile inputFile, SourceFile squidFile, SensorContext context){
    FunctionCount c = complexFunctionsPerFile.get(squidFile);
    if (c == null){
      c = new FunctionCount();
      c.countBelowThreshold = 0;
      c.countOverThreshold = 0;
    }

    context.<Integer>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS)
      .on(inputFile)
      .withValue(c.countOverThreshold)
      .save();

    context.<Double>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_PERC)
      .on(inputFile)
      .withValue(calculatePercentual(c.countOverThreshold, c.countBelowThreshold))
      .save();
  }

  private void publishLocInComplexFunctionMetricsForFile(InputFile inputFile, SourceFile squidFile, SensorContext context){
    FunctionCount locCount = locInComplexFunctionsPerFile.get(squidFile);

    if (locCount == null){
      locCount = new FunctionCount();
      locCount.countBelowThreshold = 0;
      locCount.countOverThreshold = 0;
    }

    context.<Integer>newMeasure()
            .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_LOC)
            .on(inputFile)
            .withValue(locCount.countOverThreshold)
            .save();

    context.<Double>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_LOC_PERC)
      .on(inputFile)
      .withValue(calculatePercentual(locCount.countOverThreshold, locCount.countBelowThreshold))
      .save();
  }

  @Override
  public void publishMeasureForProject(InputModule module, SensorContext context) {
    publishComplexFunctionMetrics(module, context);
    publishLinesOfCodeInComplexFunctionMetrics(module, context);
  }

  private void publishComplexFunctionMetrics(InputModule module, SensorContext context){
    context.<Integer>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS)
      .on(module)
      .withValue(functionsOverThreshold)
      .save();

    context.<Double>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_PERC)
      .on(module)
      .withValue(calculatePercentual(functionsOverThreshold, functionsBelowThreshold))
      .save();
  }

  private void publishLinesOfCodeInComplexFunctionMetrics(InputModule module, SensorContext context){
    context.<Integer>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_LOC)
      .on(module)
      .withValue(linesOfCodeOverThreshold)
      .save();

    context.<Double>newMeasure()
      .forMetric(FunctionComplexityMetrics.COMPLEX_FUNCTIONS_LOC_PERC)
      .on(module)
      .withValue(calculatePercentual(linesOfCodeOverThreshold, linesOfCodeBelowThreshold))
      .save();
  }

  private double calculatePercentual(int overThreshold, int belowThreshold){
    double total = (double)overThreshold + (double)belowThreshold;
    if (total > 0) {
      return (overThreshold * 100.0) / ((double)overThreshold + (double)belowThreshold);
    }
    else {
      return 0;
    }
  }

}
