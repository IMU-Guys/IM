package com.imuguys.hotfix.stub;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import com.android.build.gradle.AppExtension;

public class GuysStubMakerPlugin implements Plugin<Project> {
  @Override
  public void apply(@NotNull Project project) {
    System.out.println("GuysStubMakerPlugin apply");
    project.getExtensions().getByType(AppExtension.class).registerTransform(new GuysStubMakerTransform(project));
  }
}
