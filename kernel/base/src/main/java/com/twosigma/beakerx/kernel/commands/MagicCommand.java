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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.twosigma.beakerx.kernel.commands.MagicCommandFinder.find;
import static com.twosigma.beakerx.mimetype.MIMEContainer.HTML;
import static com.twosigma.beakerx.mimetype.MIMEContainer.JavaScript;
import static com.twosigma.beakerx.mimetype.MIMEContainer.Text;
import static java.util.Collections.singletonList;

import com.google.common.collect.Lists;
import com.twosigma.beakerx.jvm.object.SimpleEvaluationObject.EvaluationStatus;
import com.twosigma.beakerx.kernel.Code;
import com.twosigma.beakerx.kernel.CodeWithoutCommand;
import com.twosigma.beakerx.kernel.ImportPath;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.KernelParameters;
import com.twosigma.beakerx.kernel.PathToJar;
import com.twosigma.beakerx.kernel.commands.item.MagicCommandItem;
import com.twosigma.beakerx.kernel.commands.item.MagicCommandItemWithCode;
import com.twosigma.beakerx.kernel.commands.item.MagicCommandItemWithReply;
import com.twosigma.beakerx.kernel.commands.item.MagicCommandItemWithResult;
import com.twosigma.beakerx.kernel.commands.item.MagicCommandItemWithResultAndCode;
import com.twosigma.beakerx.kernel.msg.MessageCreator;
import com.twosigma.beakerx.message.Message;
import com.twosigma.beakerx.mimetype.MIMEContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.text.StrMatcher;
import org.apache.commons.text.StrTokenizer;

/**
 * executes magic commands and sends message
 *
 * @author lasha
 */
public class MagicCommand {

  public static final String JAVASCRIPT = "%%javascript";
  public static final String HTML = "%%html";
  public static final String BASH = "%%bash";
  public static final String LSMAGIC = "%lsmagic";
  public static final String CLASSPATH = "%classpath";
  public static final String CLASSPATH_ADD_JAR = CLASSPATH + " add jar";
  public static final String CLASSPATH_REMOVE = CLASSPATH + " remove";
  public static final String CLASSPATH_SHOW = CLASSPATH;
  public static final String CLASSPATH_ADD_MVN = CLASSPATH + " add mvn";
  public static final String ADD_MVN_FORMAT_ERROR_MESSAGE = "Wrong command format, should be " + CLASSPATH_ADD_MVN + " group name version";
  public static final String IMPORT = "%import";
  public static final String ADD_STATIC_IMPORT = IMPORT + " static";
  public static final String UNIMPORT = "%unimport";
  public static final String DEFAULT_DATASOURCE = "%defaultDatasource";

  public static final String DATASOURCES = "%datasources";
  public static final String USAGE_ERROR_MSG = "UsageError: %s is a cell magic, but the cell body is empty.";
  public static final String WRONG_FORMAT_MSG = "Wrong format. ";

  private MessageCreator messageCreator;
  private KernelFunctionality kernel;

  public MagicCommand(KernelFunctionality kernel) {
    this.kernel = checkNotNull(kernel);
    messageCreator = new MessageCreator(this.kernel);
  }

  public MagicCommandResult process(Code code, Message message, int executionCount) {
    MagicCommandFinder finder = find(code, kernel.getMagicCommands(), message, executionCount,
            this.messageCreator);
    MagicCommandResult result = new MagicCommandResult();
    if (finder.hasErrors()) {
      finder.getErrors().forEach(result::addItem);
    } else {
      List<String> functionalitiesToRun = finder.getCommands();
      functionalitiesToRun.
              forEach(item -> {
                MagicCommandItem magicCommandResultItem = finder.get(item)
                        .process(code, item, message, executionCount);
                result.addItem(magicCommandResultItem);
              });
    }
    return result;
  }

  public MagicCommandFunctionality defaultDataSources() {
    return dataSource(DEFAULT_DATASOURCE);
  }

  public MagicCommandFunctionality dataSources() {
    return dataSource(DATASOURCES);
  }

  private MagicCommandFunctionality dataSource(String source) {
    return (code, command, message, executionCount) -> {
      String[] parts = command.split(" ");
      if (parts.length != 2) {
        return sendErrorMessage(message, WRONG_FORMAT_MSG, executionCount);
      } else if (!parts[1].contains("jdbc:")) {
        return sendErrorMessage(message, "Incorrect jdbc url.", executionCount);
      }

      HashMap<String, Object> params = new HashMap<>();
      params.put(source, parts[1]);
      this.kernel.setShellOptions(new KernelParameters(params));
      return getMagicCommandItem(code, message, executionCount);
    };
  }

  public MagicCommandFunctionality addStaticImport() {
    return (code, command, message, executionCount) -> {
      String[] parts = command.split(" ");
      if (parts.length != 3) {
        sendErrorMessage(message, WRONG_FORMAT_MSG, executionCount);
      }

      this.kernel.addImport(new ImportPath(parts[1] + " " + parts[2]));
      return getMagicCommandItem(code, message, executionCount);
    };
  }

  public MagicCommandFunctionality addImport() {
    return (code, command, message, executionCount) -> {
      String[] parts = command.split(" ");
      if (parts.length != 2) {
        return createResultWithCustomMessage(kernel.getImports().toString(), message, executionCount);
      }

      this.kernel.addImport(new ImportPath(parts[1]));

      if (isValidImport(parts[1], executionCount)) {
        return getMagicCommandItem(code, message, executionCount);
      }

      this.kernel.removeImport(new ImportPath(parts[1]));

      return sendErrorMessage(message, "Could not import " + parts[1] + ", class not found.",
          executionCount);
    };
  }

  private boolean isValidImport(String part, int executionCount) {
    try {
      CompletableFuture<Boolean> validImportFuture = new CompletableFuture<>();
      kernel.executeCode("", new Message(), executionCount,
          seo -> validImportFuture.complete(!seo.getStatus().equals(EvaluationStatus.ERROR)));

      return validImportFuture.get();
    } catch (InterruptedException | ExecutionException e) {
     return Boolean.FALSE;
    }
  }

  private MagicCommandItem createResultWithCustomMessage(String customMessage, Message message, int executionCount) {
    return new MagicCommandItemWithResult(
            messageCreator
                    .buildOutputMessage(message, customMessage, false),
            messageCreator.buildReplyWithoutStatus(message, executionCount));
  }

  public MagicCommandFunctionality unimport() {
    return (code, command, message, executionCount) -> {
      String[] parts = command.split(" ");
      if (parts.length != 2) {
        return sendErrorMessage(message, WRONG_FORMAT_MSG, executionCount);
      }
      this.kernel.removeImport(new ImportPath(parts[1]));
      return getMagicCommandItem(code, message, executionCount);
    };
  }

  public MagicCommandFunctionality classpathShow() {
    return (code, command, message, executionCount) -> {
      MIMEContainer result = Text(kernel.getClasspath());

      if (code.takeCodeWithoutCommand().isPresent()) {
        return new MagicCommandItemWithResultAndCode(
                messageCreator.buildOutputMessage(message, result.getData().toString(), false),
                messageCreator.buildReplyWithoutStatus(message, executionCount),
                code.takeCodeWithoutCommand().get());
      }

      return createResultWithCustomMessage(result.getData().toString(), message, executionCount);
    };
  }

  public MagicCommandFunctionality classpathRemove() {
    return (code, command, message, executionCount) -> null;
  }

  public MagicCommandFunctionality classpathAddJar() {
    return (code, command, message, executionCount) -> {
      String[] split = splitPath(command);
      if (split.length != 4) {
        return sendErrorMessage(message, WRONG_FORMAT_MSG + CLASSPATH_ADD_JAR, executionCount);
      }

      String path = split[3];
      ErrorData errorData = isValidPath(path);

      if (errorData.hasError()) {
        return sendErrorMessage(message, errorData.getMessage(), executionCount);
      } else {
        return getMagicCommandItem(addJars(path), code, message, executionCount);
      }
    };
  }

  public MagicCommandFunctionality classpathAddMvn(MavenJarResolver.ResolverParams commandParams) {
    return (code, command, message, executionCount) -> {
      String[] split = splitPath(command);
      if (split.length != 6) {
        return sendErrorMessage(message, ADD_MVN_FORMAT_ERROR_MESSAGE, executionCount);
      }
      MavenJarResolver classpathAddMvnCommand = new MavenJarResolver(commandParams);
      MavenJarResolver.AddMvnCommandResult result = classpathAddMvnCommand.retrieve(split[3], split[4], split[5]);
      if (result.isJarRetrieved()) {
        return getMagicCommandItem(addJars(classpathAddMvnCommand.getPathToMavenRepo() + "/*"), code, message, executionCount);
      }
      return sendErrorMessage(message, result.getErrorMessage(), executionCount);
    };
  }

  private String[] splitPath(String command) {
    StrTokenizer tokenizer = new StrTokenizer(command, StrMatcher.spaceMatcher(), StrMatcher.quoteMatcher());

    return tokenizer.getTokenArray();
  }

  private MagicCommandItemWithResult sendErrorMessage(Message message, String messageText, int executionCount) {
    return new MagicCommandItemWithResult(
            messageCreator
                    .buildOutputMessage(message, messageText, true),
            messageCreator.buildReplyWithoutStatus(message, executionCount)
    );
  }

  private Collection<String> addJars(String path) {
    List<String> addedJarsName = Lists.newLinkedList();

    if (doesPathContainsWildCards(path)) {
      Map<Path, String> paths = getPaths(path);
      List<PathToJar> pathsToJars = paths.keySet().stream()
              .map(currentPath -> new PathToJar(currentPath.toString()))
              .collect(Collectors.toList());

      List<Path> addedPaths = kernel.addJarsToClasspath(pathsToJars);
      addedJarsName.addAll(addedPaths.stream().map(x -> x.getFileName().toString()).collect(Collectors.toList()));

    } else {
      Path currentPath = Paths.get(path);
      if (this.kernel.addJarToClasspath(new PathToJar(path))) {
        addedJarsName.add(currentPath.getFileName().toString());
      }
    }

    return addedJarsName;
  }

  private Boolean containsSingleWildcardSymbol(String path) {
    return path.length() - path.replace("*", "").length() == 1;
  }

  private Map<Path, String> getPaths(String pathWithWildcard) {
    String pathWithoutWildcards = pathWithWildcard.replace("*", "");
    try {

      return Files.list(Paths.get(pathWithoutWildcards))
              .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
              .collect(Collectors.toMap(p -> p, o -> o.getFileName().toString()));

    } catch (IOException e) {
      throw new IllegalStateException("Cannot find any jars files in selected path");
    }
  }

  private Boolean doesPathContainsWildCards(String path) {
    return path.contains("*");
  }

  private MagicCommandItem getMagicCommandItem(Collection<String> newAddedJars, Code code, Message message, int executionCount) {
    if (newAddedJars.isEmpty()) {
      return getMagicCommandItem(code, message, executionCount);
    }

    String textMessage = "Added jar" + (newAddedJars.size() > 1 ? "s: " : ": ") + newAddedJars + "\n";

    if (code.takeCodeWithoutCommand().isPresent()) {
      return new MagicCommandItemWithResultAndCode(
              messageCreator.buildOutputMessage(message, textMessage, false),
              messageCreator.buildReplyWithoutStatus(message, executionCount),
              code.takeCodeWithoutCommand().get());
    }

    return new MagicCommandItemWithResult(
            messageCreator
                    .buildOutputMessage(message, textMessage, false),
            messageCreator.buildReplyWithoutStatus(message, executionCount));
  }

  private MagicCommandItem getMagicCommandItem(Code code, Message message, int executionCount) {
    if (code.takeCodeWithoutCommand().isPresent()) {
      return new MagicCommandItemWithCode(code.takeCodeWithoutCommand().get());
    }

    return new MagicCommandItemWithReply(
            messageCreator.buildReplyWithoutStatus(message, executionCount));
  }

  public MagicCommandFunctionality javascript() {
    return (code, command, message, executionCount) -> {
      MIMEContainer result = JavaScript(code.takeCodeWithoutCommand().get().asString());
      return new MagicCommandItemWithResult(
              messageCreator
                      .buildMessage(message, singletonList(result), executionCount),
              messageCreator.buildReplyWithoutStatus(message, executionCount)
      );
    };
  }

  public MagicCommandFunctionality html() {
    return (code, command, message, executionCount) -> code.takeCodeWithoutCommand()
            .map(codeWithoutCommand -> {
              MIMEContainer html = HTML(
                      "<html>" + codeWithoutCommand.asString() + "</html>");
              return new MagicCommandItemWithResult(
                      messageCreator
                              .buildMessage(message, singletonList(html), executionCount),
                      messageCreator.buildReplyWithoutStatus(message, executionCount)
              );
            }).orElse(sendErrorMessage(message, String.format(USAGE_ERROR_MSG, HTML), executionCount));
  }

  public MagicCommandFunctionality bash() {
    return (code, command, message, executionCount) -> code.takeCodeWithoutCommand().map(codeWithoutCommand -> {

      ErrorData errorData = executeBashCode(codeWithoutCommand);

      if (errorData.hasError()) {
        return sendErrorMessage(message, errorData.getMessage(), executionCount);
      }

      return new MagicCommandItemWithResult(
              messageCreator.buildOutputMessage(message, errorData.getMessage(), false),
              messageCreator.buildReplyWithoutStatus(message, executionCount)
      );
    }).orElse(sendErrorMessage(message, String.format(USAGE_ERROR_MSG, BASH), executionCount));
  }

  public MagicCommandFunctionality lsmagic() {
    return (code, command, message, executionCount) -> {
      String result = "Available magic commands:\n";

      result += kernel.getMagicCommands().stream()
              .map(commandType -> commandType.getCommand() + " " + commandType.getParameters())
              .collect(Collectors.joining("\n"));

      return new MagicCommandItemWithResult(
              messageCreator.buildOutputMessage(message, result, false),
              messageCreator.buildReplyWithoutStatus(message, executionCount)
      );
    };
  }

  private ErrorData executeBashCode(CodeWithoutCommand code) {
    String[] cmd = {"/bin/bash", "-c", code.asString()};
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    StringBuilder output = new StringBuilder();
    try {
      Process process = pb.start();
      process.waitFor();
      String line;
      BufferedReader reader = new BufferedReader(new InputStreamReader(
              process.getInputStream()));
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
      process.destroy();
    } catch (IOException | InterruptedException e) {
      return new ErrorData(true, e.getMessage());
    }

    return new ErrorData(false, output.toString());
  }

  private ErrorData isValidPath(String path) {
    boolean isEmpty = checkNotNull(path).isEmpty();
    if (isEmpty) {
      return new ErrorData(true, "Please provide a path");
    }

    if (doesPathContainsWildCards(path)) {
      if (!containsSingleWildcardSymbol(path) || !path.endsWith("*")) {
        return new ErrorData(true, "Bad classpath wildcard syntax, path can only end with *");
      } else if (!Paths.get(path.replace("*", "")).toFile().exists()) {
        return new ErrorData(true, "Bad classpath, directory cannot be find");
      }
    } else if (!Paths.get(path).toFile().exists()) {
      return new ErrorData(true, "Bad classpath, file not found");
    }

    return new ErrorData(false, "");
  }

  private class ErrorData {

    public ErrorData(boolean hasError, String message) {
      this.error = hasError;
      this.message = message;
    }

    boolean error;
    String message;

    public boolean hasError() {
      return error;
    }

    public String getMessage() {
      return message;
    }
  }
}
