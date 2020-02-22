package com.imuguys.hotfix.patch;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GuysPatchMakerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("GuysPatchMakerPlugin apply");
        project.getExtensions().getByType(AppExtension.class).registerTransform(new GuysPatchMakerTransform(project));
    }
}
