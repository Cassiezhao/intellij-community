// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.push;

import com.intellij.dvcs.push.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgOutgoingCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.log.HgBaseLogParser;
import org.zmlx.hg4idea.log.HgHistoryUtil;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgChangesetUtil;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HgOutgoingCommitsProvider extends OutgoingCommitsProvider<HgRepository, HgPushSource, HgTarget> {


  private static final Logger LOG = Logger.getInstance(HgOutgoingCommitsProvider.class);
  private static final String LOGIN_AND_REFRESH_LINK = "Enter Password & Refresh";

  @NotNull
  @Override
  public OutgoingResult getOutgoingCommits(@NotNull final HgRepository repository,
                                           @NotNull final PushSpec<HgPushSource, HgTarget> pushSpec,
                                           boolean initial) {
    final Project project = repository.getProject();
    HgVcs hgvcs = HgVcs.getInstance(project);
    assert hgvcs != null;
    final HgVersion version = hgvcs.getVersion();
    String[] templates = HgBaseLogParser.constructFullTemplateArgument(true, version);
    HgOutgoingCommand hgOutgoingCommand = new HgOutgoingCommand(project);
    HgTarget hgTarget = pushSpec.getTarget();
    List<VcsError> errors = new ArrayList<>();
    if (StringUtil.isEmptyOrSpaces(hgTarget.myTarget)) {
      errors.add(new VcsError("Hg push path could not be empty."));
      return new OutgoingResult(Collections.emptyList(), errors);
    }
    HgCommandResult result = hgOutgoingCommand
      .execute(repository.getRoot(), HgChangesetUtil.makeTemplate(templates), pushSpec.getSource().getPresentation(),
               hgTarget.myTarget, initial);
    if (result == null) {
      errors.add(new VcsError("Couldn't execute hg outgoing command for " + repository));
      return new OutgoingResult(Collections.emptyList(), errors);
    }
    List<String> resultErrors = result.getErrorLines();
    if (resultErrors != null && !resultErrors.isEmpty() && result.getExitValue() != 0) {
      for (String error : resultErrors) {
        if (HgErrorUtil.isAbortLine(error)) {
          if (HgErrorUtil.isAuthorizationError(error)) {
            VcsError authorizationError =
              new VcsError(error + "<a href='authenticate'>" + LOGIN_AND_REFRESH_LINK + "</a>", new VcsErrorHandler() {
                @Override
                public void handleError(@NotNull CommitLoader commitLoader) {
                  commitLoader.reloadCommits();
                }
              });
            errors.add(authorizationError);
          }
          else {
            errors.add(new VcsError(error));
          }
        }
      }
      LOG.warn(resultErrors.toString());
    }
    return new OutgoingResult(HgHistoryUtil.createFullCommitsFromResult(project, repository.getRoot(), result, version, true), errors);
  }
}
