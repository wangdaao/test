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

package org.sonar.cxx.sensors.functionsize;

import static java.util.Arrays.asList;
import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class FunctionSizeMetrics implements Metrics {

  public static final Metric<Integer> BIG_FUNCTIONS = new Metric.Builder("big_functions", "Big Functions", Metric.ValueType.INT)
    .setDescription("Number of functions with too many lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.FALSE)
    .setDomain(CoreMetrics.DOMAIN_SIZE)
    .create();

  public static final Metric<Integer> BIG_FUNCTIONS_LOC = new Metric.Builder("loc_in_big_functions", "Big Functions Lines of Code", Metric.ValueType.INT)
    .setDescription("Number of lines of code in functions with too many lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.FALSE)
    .setDomain(CoreMetrics.DOMAIN_SIZE)
    .create();

  public static final Metric<Double> BIG_FUNCTIONS_PERC = new Metric.Builder("perc_big_functions", "Big Functions (%)", Metric.ValueType.PERCENT)
    .setDescription("% of functions with too many lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.FALSE)
    .setDomain(CoreMetrics.DOMAIN_SIZE)
    .create();

  public static final Metric<Double> BIG_FUNCTIONS_LOC_PERC = new Metric.Builder("perc_loc_in_big_functions", "Big Functions Lines of Code (%)", Metric.ValueType.PERCENT)
    .setDescription("% of lines of code in functions with too many lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.FALSE)
    .setDomain(CoreMetrics.DOMAIN_SIZE)
    .create();

   public static final Metric<Integer> LOC_IN_FUNCTIONS = new Metric.Builder("loc_in_functions", "Lines of Code in Functions", Metric.ValueType.INT)
    .setDescription("Number of lines of code in function bodies")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.FALSE)
    .setDomain(CoreMetrics.DOMAIN_SIZE)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return asList(BIG_FUNCTIONS, BIG_FUNCTIONS_PERC, BIG_FUNCTIONS_LOC, BIG_FUNCTIONS_LOC_PERC, LOC_IN_FUNCTIONS);
  }

}
