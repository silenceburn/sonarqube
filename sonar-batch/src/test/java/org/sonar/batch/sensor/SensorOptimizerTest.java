/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.sensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SensorOptimizerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultFileSystem fs;
  private SensorOptimizer optimizer;
  private Settings settings;
  private AnalysisMode analysisMode;

  @Before
  public void prepare() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    settings = new Settings();
    analysisMode = mock(AnalysisMode.class);
    optimizer = new SensorOptimizer(fs, new ActiveRulesBuilder().build(), settings, analysisMode);
  }

  @Test
  public void should_run_analyzer_with_no_metadata() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_language() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnLanguages("java", "php");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("foo", "src/Foo.java").setLanguage("java"));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_type() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnFileType(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("foo", "tests/FooTest.java").setType(InputFile.Type.TEST));
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("foo", "src/Foo.java").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_both_type_and_language() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnLanguages("java", "php")
      .onlyOnFileType(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("foo", "tests/FooTest.java").setLanguage("java").setType(InputFile.Type.TEST));
    fs.add(new DefaultInputFile("foo", "src/Foo.cbl").setLanguage("cobol").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("foo", "src/Foo.java").setLanguage("java").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_repository() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .createIssuesForRuleRepositories("squid");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    ActiveRules activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("repo1", "foo"))
      .activate()
      .build();
    optimizer = new SensorOptimizer(fs, activeRules, settings, analysisMode);

    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("repo1", "foo"))
      .activate()
      .create(RuleKey.of("squid", "rule"))
      .activate()
      .build();
    optimizer = new SensorOptimizer(fs, activeRules, settings, analysisMode);
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_settings() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .requireProperty("sonar.foo.reportPath");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    settings.setProperty("sonar.foo.reportPath", "foo");
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_disabled_in_preview() throws Exception {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .disabledInPreview();
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();

    when(analysisMode.isPreview()).thenReturn(true);

    assertThat(optimizer.shouldExecute(descriptor)).isFalse();
  }
}
