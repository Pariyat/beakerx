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
package com.twosigma.beakerx.kernel.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.twosigma.beakerx.KernelTest;
import com.twosigma.beakerx.evaluator.EvaluatorTest;
import com.twosigma.beakerx.kernel.Code;
import com.twosigma.beakerx.kernel.CodeWithoutCommand;
import com.twosigma.beakerx.kernel.ImportPath;
import com.twosigma.beakerx.message.Message;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ImportMagicCommandTest {

  private MagicCommand sut;
  private KernelTest kernel;

  @Before
  public void setUp() throws Exception {
    this.kernel = new KernelTest("id2", new EvaluatorTest());
    this.sut = new MagicCommand(kernel);
  }

  @After
  public void tearDown() throws Exception {
    kernel.exit();
  }

  @Test
  public void addImport() throws Exception {
    //given
    Code code = new Code("" +
            "%import com.twosigma.beakerx.widgets.integers.IntSlider\n" +
            "w = new IntSlider()");
    //when
    MagicCommandResult result = sut.process(code, new Message(), 1);
    //then
    assertThat(result.getItems().get(0).getCode().get()).isEqualTo(new CodeWithoutCommand("w = new IntSlider()"));
    assertThat(kernel.getImports().getImportPaths()).contains(new ImportPath("com.twosigma.beakerx.widgets.integers.IntSlider"));
  }

  @Test
  public void removeImport() throws Exception {
    //given
    Code addImport = new Code("" +
            "%import com.twosigma.beakerx.widgets.integers.IntSlider\n");
    sut.process(addImport, new Message(), 1);
    assertThat(kernel.getImports().getImportPaths()).contains(new ImportPath("com.twosigma.beakerx.widgets.integers.IntSlider"));
    //when
    Code removeImport = new Code("" +
            "%unimport com.twosigma.beakerx.widgets.integers.IntSlider\n");
    sut.process(removeImport, new Message(), 1);
    //then
    assertThat(kernel.getImports().getImportPaths()).doesNotContain(new ImportPath("com.twosigma.beakerx.widgets.integers.IntSlider"));
  }

  @Test
  public void allowExtraWhitespaces() {
    MagicCommandResult result = sut
        .process(new Code("%import       com.twosigma.beakerx.widgets.integers.IntSlider"), new Message(), 1);

    assertThat(kernel.getImports().getImportPaths()).contains(new ImportPath("com.twosigma.beakerx.widgets.integers.IntSlider"));
  }

  @Test
  public void wrongImportFormat() {
    Code wrongFormatImport = new Code("%import ");
    MagicCommandResult process = sut.process(wrongFormatImport, new Message(), 1);

    Optional<Message> result = process.getItems().get(0).getResult();
    assertThat(result.isPresent()).isTrue();
    result.ifPresent(r -> {
      Map<String, Serializable> content = r.getContent();
      assertThat(content.get("text").equals("Wrong import format."));
    });
  }

}
