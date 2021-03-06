/*
  Copyright (C), 2018-2020, ZhangYuanSheng
  FileName: GotoRequestAction
  Author:   ZhangYuanSheng
  Date:     2020/5/12 16:22
  Description: 
  History:
  <author>          <time>          <version>          <desc>
  作者姓名            修改时间           版本号              描述
 */
package core.view.search;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import core.beans.RequestMethod;
import core.view.Icons;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.List;

/**
 * @author ZhangYuanSheng
 * @version 1.0
 */
public class GotoRequestAction extends GotoActionBase implements DumbAware {

    @Override
    protected void gotoActionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 显示featureId对应的Tips
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.service");

        ChooseByNameContributor[] contributors = {
                new GotoRequestContributor(e.getData(LangDataKeys.MODULE)),
        };

        RequestFilteringGotoByModel model = new RequestFilteringGotoByModel(project, contributors);

        GotoActionCallback<RequestMethod> callback = new GotoActionCallback<RequestMethod>() {

            @NotNull
            @Contract("_ -> new")
            @Override
            protected ChooseByNameFilter<RequestMethod> createFilter(@NotNull ChooseByNamePopup popup) {
                return new GotoRequestMappingFilter(popup, model, project);
            }

            @Override
            public void elementChosen(ChooseByNamePopup chooseByNamePopup, Object element) {
                if (element instanceof RestServiceItem) {
                    RestServiceItem navigationItem = (RestServiceItem) element;
                    if (navigationItem.canNavigate()) {
                        navigationItem.navigate(true);
                    }
                }
            }
        };

        GotoRequestProvider provider = new GotoRequestProvider(getPsiContext(e));
        showNavigationPopup(
                e, model, callback,
                "Request Mapping Url matching pattern",
                true,
                true,
                (ChooseByNameItemProvider) provider
        );
    }

    @Override
    protected <T> void showNavigationPopup(@NotNull AnActionEvent e,
                                           @NotNull ChooseByNameModel model,
                                           final GotoActionCallback<T> callback,
                                           @Nullable final String findUsagesTitle,
                                           boolean useSelectionFromEditor,
                                           final boolean allowMultipleSelection,
                                           final ChooseByNameItemProvider itemProvider) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        //noinspection ConstantConditions
        boolean mayRequestOpenInCurrentWindow = model.willOpenEditor() &&
                FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        Pair<String, Integer> start = getInitialText(useSelectionFromEditor, e);
        String predefinedText = start.first == null ? tryFindCopiedUrl() : start.first;
        showNavigationPopup(callback, findUsagesTitle,
                            RestChooseByNamePopup.createPopup(project, model, itemProvider, predefinedText,
                                                              mayRequestOpenInCurrentWindow,
                                                              start.second),
                            allowMultipleSelection);
    }

    @Nullable
    private String tryFindCopiedUrl() {
        String contents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (contents == null) {
            return null;
        }

        contents = contents.trim();
        if (contents.startsWith("http")) {
            if (contents.length() <= 120) {
                return contents;
            } else {
                return contents.substring(0, 120);
            }
        }

        return null;
    }

    protected static class GotoRequestMappingFilter extends ChooseByNameFilter<RequestMethod> {

        GotoRequestMappingFilter(final ChooseByNamePopup popup,
                                 RequestFilteringGotoByModel model, final Project project) {
            super(popup, model, GotoRequestConfiguration.getInstance(project), project);
        }

        @Override
        @NotNull
        protected List<RequestMethod> getAllFilterValues() {
            return Arrays.asList(RequestMethod.values());
        }

        @Override
        protected String textForFilterValue(@NotNull RequestMethod value) {
            return value.name();
        }

        @Override
        protected Icon iconForFilterValue(@NotNull RequestMethod value) {
            return Icons.getMethodIcon(value);
        }
    }
}
