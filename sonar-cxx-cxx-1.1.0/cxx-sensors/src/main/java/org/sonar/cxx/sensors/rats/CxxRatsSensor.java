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
package org.sonar.cxx.sensors.rats;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.CxxReportIssue;
import org.sonar.cxx.sensors.utils.CxxReportSensor;
import org.sonar.cxx.sensors.utils.CxxUtils;

/**
 * {@inheritDoc}
 */
public class CxxRatsSensor extends CxxReportSensor {

  private static final Logger LOG = Loggers.get(CxxRatsSensor.class);
  private static final String MISSING_RATS_TYPE = "fixed size global buffer";
  public static final String REPORT_PATH_KEY = "rats.reportPath";
  public static final String KEY = "Rats";

  /**
   * CxxRatsSensor for RATS Sensor
   *
   * @param language defines settings C or C++
   */
  public CxxRatsSensor(CxxLanguage language) {
    super(language);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name(language.getName() + " RatsSensor")
      .onlyOnLanguage(this.language.getKey())
      .createIssuesForRuleRepository(CxxRatsRuleRepository.KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(getReportPathKey()));
  }

  @Override
  public String getReportPathKey() {
    return this.language.getPluginProperty(REPORT_PATH_KEY);
  }

  @Override
  protected void processReport(final SensorContext context, File report)
    throws org.jdom.JDOMException, java.io.IOException {
    LOG.debug("Parsing 'RATS' format");

    try {
      SAXBuilder builder = new SAXBuilder(false);
      Element root = builder.build(report).getRootElement();
      @SuppressWarnings("unchecked")
      List<Element> vulnerabilities = root.getChildren("vulnerability");
      for (Element vulnerability : vulnerabilities) {
        String type = getVulnerabilityType(vulnerability.getChild("type"));
        String message = vulnerability.getChild("message").getTextTrim();

        @SuppressWarnings("unchecked")
        List<Element> files = vulnerability.getChildren("file");

        for (Element file : files) {
          String fileName = file.getChild("name").getTextTrim();

          @SuppressWarnings("unchecked")
          List<Element> lines = file.getChildren("line");
          for (Element lineElem : lines) {
            String line = lineElem.getTextTrim();

            CxxReportIssue issue = new CxxReportIssue(CxxRatsRuleRepository.KEY, type, fileName, line, message);
            saveUniqueViolation(context, issue);
          }
        }
      }
    } catch (org.jdom.input.JDOMParseException e) {
      // when RATS fails the XML file might be incomplete
      LOG.error("Ignore incomplete XML output from RATS '{}'", CxxUtils.getStackTrace(e));
    }
  }

  private static String getVulnerabilityType(@Nullable Element child) {
    if (child != null) {
      return child.getTextTrim();
    }
    return MISSING_RATS_TYPE;
  }

  @Override
  protected String getSensorKey() {
    return KEY;
  }
}
