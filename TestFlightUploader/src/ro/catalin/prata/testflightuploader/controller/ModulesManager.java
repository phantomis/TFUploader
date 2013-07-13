package ro.catalin.prata.testflightuploader.controller;

/*  Copyright 2013 Catalin Prata

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;

/**
 * Description
 *
 * @author Catalin Prata
 *         Date: 7/9/13
 */
public class ModulesManager {

    public static final String ANDROID_VERSION_CODE = "android:versionCode";
    public static final String ANDROID_VERSION_NAME = "android:versionName";
    /**
     * Manager's single instance
     */
    private static ModulesManager sInstance = null;

    private ModulesManager() {

    }

    public static ModulesManager instance() {

        if (sInstance == null) {
            sInstance = new ModulesManager();
        }

        return sInstance;
    }

    /**
     * Returns the given module's apk path
     *
     * @param module android module used to get the facet and the apk file path
     * @return file path of the android apk for the given module
     */
    public String getAndroidApkPath(Module module) {

        return AndroidRootUtil.getApkPath(AndroidFacet.getInstance(module));

    }

    /**
     * Returns an array of module names for the current project
     *
     * @return array of module names
     */
    public String[] getAllModuleNamesForCurrentProject() {

        Module[] modules = getModulesForTheMainProject();
        String[] moduleNames = new String[modules.length];

        int index = 0;
        for (Module module : modules) {

            moduleNames[index] = module.getName();
            index++;
        }

        return moduleNames;

    }

    /**
     * Returns all modules found in the main project,
     * which is the first opened project if there are more than one projects opened at a time
     *
     * @return array of modules for the main project
     */
    public Module[] getModulesForTheMainProject() {
        Module[] modules = ModuleManager.getInstance(ProjectManager.getInstance().getOpenProjects()[0]).getSortedModules();
        Module[] sortedModules = new Module[modules.length];

        // used to go back from the last module to the first one
        int reverseIndex = modules.length - 1;

        // loop through all the modules and add the in a reverse order in the sorted array
        for (int index = 0; index < modules.length; index++) {
            sortedModules[index] = modules[reverseIndex];
            reverseIndex--;
        }

        return sortedModules;
    }

    /**
     * Returns the index of the selected module in the project modules list, if the module is not present here, 0 is returned
     *
     * @param moduleName module name
     * @return 0 if the module is not in the list of the current project
     */
    public int getSelectedModuleIndex(String moduleName) {
        Module[] modules = getModulesForTheMainProject();

        if (modules == null) {
            return 0;
        }

        int index = 0;
        for (Module module : modules) {

            if (module.getName().equals(moduleName)) {
                return index;
            }

            index++;
        }

        return 0;
    }

    /**
     * Returns a module from the main project that has the given name
     *
     * @return module with the given name or null if not found in this project
     */
    public Module getModuleByName(String moduleName) {
        Module[] modules = getModulesForTheMainProject();

        if (modules == null) {
            return null;
        }

        for (Module module : modules) {

            if (module.getName().equals(moduleName)) {
                return module;
            }

        }

        return null;

    }

    /**
     * Returns the module that is not(or the less) used by the other modules, this way we avoid getting library modules
     *
     * @return the most important module, which can be a non library module
     */
    public Module getMostImportantModule() {

        Module[] modules = getModulesForTheMainProject();

        if (modules == null) {
            return null;
        } else {

            // the last one is the one that is not used by the other modules
            // or is the most absent from the other modules dependency
            return modules[modules.length - 1];

        }

    }

    /**
     * Returns the manifest file for the given module
     *
     * @param module module to search the manifest document for
     * @return manifest doc for the given module
     */
    public Manifest getManifestForModule(final Module module) {

        return AndroidFacet.getInstance(module).getManifest();

    }

    /**
     * Returns the build version name from the given manifest file
     *
     * @param manifest manifest file that will be searched for the build version name
     * @return the current manifest version name value
     */
    public String getBuildVersionName(Manifest manifest) {
        return manifest.getXmlTag().getAttribute(ANDROID_VERSION_NAME).getValue();
    }

    /**
     * Set the build version name in the given manifest file,
     * note that this action is not performed in a write action environment so it should be called inside an
     * ApplicationManager.getApplication().runWriteAction() method
     *
     * @param manifest manifest file that will be altered
     * @param newValue new version name value
     */
    private void setBuildVersionName(Manifest manifest, String newValue) {
        manifest.getXmlTag().getAttribute(ANDROID_VERSION_NAME).setValue(newValue);
    }

    /**
     * Set the build version name and code in the manifest file asynchronously as it has to run in write mode
     *
     * @param manifest            manifest file to be altered
     * @param newVersionNameValue the new version name value
     * @param newVersionCodeValue the new version code value
     * @param delegate            if != null, this will send callbacks on completion
     */
    public void setBuildVersionNameAndCode(final Manifest manifest, final String newVersionNameValue,
                                           final String newVersionCodeValue, final ManifestChangesDelegate delegate) {

        // open a write action environment so we can update the manifest file
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                // set the version name value
                setBuildVersionName(manifest, newVersionNameValue);
                // set the version code value
                setBuildVersionCode(manifest, newVersionCodeValue);

                if (delegate != null) {

                    // notify that the values were changed
                    delegate.onVersionValueFinishedUpdate();

                }

            }
        });

    }

    /**
     * Set the build version code in the given manifest file,
     * note that this action is not performed in a write action environment so it should be called inside an
     * ApplicationManager.getApplication().runWriteAction() method
     *
     * @param manifest the project manifest file
     * @param newValue the value to be set as the version code
     */
    private void setBuildVersionCode(Manifest manifest, String newValue) {

        manifest.getXmlTag().getAttribute(ANDROID_VERSION_CODE).setValue(newValue);


    }

    /**
     * Returns the code version of the given manifest file
     *
     * @param manifest android manifest dom object
     * @return android build version code
     */
    public String getBuildVersionCode(Manifest manifest) {
        return manifest.getXmlTag().getAttribute(ANDROID_VERSION_CODE).getValue();
    }

    /**
     * Used to notify manifest changes updates as each write action needs to be done on a secondary thread
     */
    public interface ManifestChangesDelegate {

        /**
         * Called after the version name and code values were changed in the manifest file
         */
        public void onVersionValueFinishedUpdate();

    }

}