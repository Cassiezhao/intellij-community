package com.jetbrains.python.sdk;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public abstract class PythonSdkFlavor {
  public Collection<String> suggestHomePaths() {
    return Collections.emptyList();
  }

  public static List<PythonSdkFlavor> getApplicableFlavors() {
    List<PythonSdkFlavor> result = new ArrayList<PythonSdkFlavor>();
    if (SystemInfo.isWindows) {
      result.add(WinPythonSdkFlavor.INSTANCE);
    }
    else if (SystemInfo.isMac) {
      result.add(MacPythonSdkFlavor.INSTANCE);
    }
    else if (SystemInfo.isUnix) {
      result.add(UnixPythonSdkFlavor.INSTANCE);
    }
    result.add(JythonSdkFlavor.INSTANCE);
    result.add(IronPythonSdkFlavor.INSTANCE);
    return result;
  }

  @Nullable
  public static PythonSdkFlavor getFlavor(String sdkPath) {
    for (PythonSdkFlavor flavor : getApplicableFlavors()) {
      if (flavor.isValidSdkHome(sdkPath)) {
        return flavor;
      }
    }
    return null;
  }

  /**
   * Checks if the path is the name of a Python interpreter of this flavor.
   *
   * @param path path to check.
   * @return true if paths points to a valid home.
   */
  public boolean isValidSdkHome(String path) {
    File file = new File(path);
    return file.isFile() && FileUtil.getNameWithoutExtension(file).toLowerCase().startsWith("python");
  }

  public String getVersionString(String sdkHome) {
    return getVersionFromOutput(sdkHome, "-V", "(Python \\S+).*", false);
  }

  protected static String getVersionFromOutput(String sdkHome, String version_opt, String version_regexp, boolean stdout) {
    Pattern pattern = Pattern.compile(version_regexp);
    String run_dir = new File(sdkHome).getParent();
    final ProcessOutput process_output = SdkUtil.getProcessOutput(run_dir, new String[]{sdkHome, version_opt});
    if (process_output.getExitCode() != 0) {
      throw new RuntimeException(process_output.getStderr() + " (exit code " + process_output.getExitCode() + ")");
    }
    final List<String> lines = stdout ? process_output.getStdoutLines() : process_output.getStderrLines();
    return SdkUtil.getFirstMatch(lines, pattern);
  }

  public Collection<String> getExtraDebugOptions() {
    return Collections.emptyList();
  }

  public void addToPythonPath(GeneralCommandLine cmd, String path) {
    addToEnv(cmd, PYTHONPATH, path);
  }

  public static void addToEnv(GeneralCommandLine cmd, final String key, String value) {
    Map<String,String> envs = cmd.getEnvParams();
    if (envs == null) {
      envs = new HashMap<String, String>();
      cmd.setEnvParams(envs);
    }
    addToEnv(envs, key, value);
  }

  public static void addToPythonPath(Map<String, String> envs, String value) {
    addToEnv(envs, PYTHONPATH, value);
  }

  public static void addToEnv(Map<String, String> envs, String key, String value) {
    if (envs.containsKey(key)) {
      envs.put(key, value + File.pathSeparatorChar + envs.get(key));
    } else {
      envs.put(key, value);
    }
  }

  private static final String PYTHONPATH = "PYTHONPATH";

  public void addPredefinedEnvironmentVariables(Map<String, String> envs) {
  }
}
