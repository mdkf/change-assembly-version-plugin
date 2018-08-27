package org.jenkinsci.plugins.changeassemblyversion;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import static java.lang.String.format;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.jenkinsci.Symbol;
import static org.jenkinsci.plugins.changeassemblyversion.ChangeTools.replaceOrAppend;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import static org.jenkinsci.plugins.tokenmacro.TokenMacro.expandAll;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author <a href="mailto:leonardo.kobus@hbsis.com.br">Leonardo Kobus</a>
 */
public class ChangeAssemblyVersion extends Builder implements SimpleBuildStep {
    private static final Logger LOG = getLogger(ChangeAssemblyVersion.class.getName());

    private final String assemblyCultureReplacementString;
    private final String assemblyTrademarkReplacementString;
    private final String assemblyCopyrightReplacementString;
    private final String assemblyProductReplacmentString;
    private final String assemblyCompanyReplacementString;
    private final String assemblyDescriptionReplacementString;
    private final String assemblyTitleReplacmentString;
    private final String assemblyFileVersionReplacementString;
    private final String assemblyInfoVersionReplacementString;
    private final String assemblyVersionReplacementString;

    private final String BASE_REGEX;
    private final String BasePattern;


    private String assemblyFile;
    private String assemblyVersion;
    private String assemblyFileVersion;
    private String assemblyInformationalVersion;
    private String assemblyTitle;
    private String assemblyDescription;
    private String assemblyCompany;
    private String assemblyProduct;
    private String assemblyCopyright;
    private String assemblyTrademark;
    private String assemblyCulture;

    private final Pattern assemblyVersionRegex;
    private final Pattern assemblyInfoVersionRegex;
    private final Pattern assemblyFileVersionRegex;
    private final Pattern assemblyTitleRegex;
    private final Pattern assemblyDescriptionRegex;
    private final Pattern assemblyCompanyRegex;
    private final Pattern assemblyProductRegex;
    private final Pattern assemblyCopyrightRegex;
    private final Pattern assemblyTrademarkRegex;
    private final Pattern assemblyCultureRegex;
    
    private String expandedAssemblyVersion;
    private String expandedAssemblyFileVersion;
    private String expandedAssemblyInfoVersion;
    private String expandedAssemblyTitle;
    private String expandedAssemblyDescription;
    private String expandedAssemblyCompany;
    private String expandedAssemblyProduct;
    private String expandedAssemblyCopyright;
    private String expandedAssemblyTrademark;
    private String expandedAssemblyCulture;
    
        /*
    private final String assemblyCultureString;
    private final String assemblyTrademarkString;
    private final String assemblyCopyrightString;
    private final String assemblyProductString;
    private final String assemblyCompanyString;
    private final String assemblyDescriptionString;
    private final String assemblyTitleString;
    private final String assemblyFileVersionString;
    private final String assemblyInfoVersionString;
    private final String assemblyVersionString; */

    @DataBoundConstructor
    public ChangeAssemblyVersion() {
        this.BasePattern = "[assembly: %s]";
        this.assemblyCultureReplacementString = format(BasePattern, "AssemblyCulture(\"%s\")");
        this.assemblyTrademarkReplacementString = format(BasePattern, "AssemblyTrademark(\"%s\")");
        this.assemblyCopyrightReplacementString = format(BasePattern, "AssemblyCopyright(\"%s\")");
        this.assemblyProductReplacmentString = format(BasePattern, "AssemblyProduct(\"%s\")");
        this.assemblyCompanyReplacementString = format(BasePattern, "AssemblyCompany(\"%s\")");
        this.assemblyDescriptionReplacementString = format(BasePattern, "AssemblyDescription(\"%s\")");
        this.assemblyTitleReplacmentString = format(BasePattern, "AssemblyTitle(\"%s\")");
        this.assemblyFileVersionReplacementString = format(BasePattern, "AssemblyFileVersion(\"%s\")");
        this.assemblyInfoVersionReplacementString = format(BasePattern, "AssemblyInformationalVersion(\"%s\")");
        this.assemblyVersionReplacementString = format(BasePattern, "AssemblyVersion(\"%s\")");

        //http://stackoverflow.com/questions/39257137/java-regex-to-filter-lines-with-comment-not-working-as-expected
        this.BASE_REGEX = "(?m)((?:\\G|^)[^\\[/\\n]*+(?:\\[(?!assembly:\\s*?%1$s\\s*?\\(\\s*?\\\".*?\\\"\\s*?\\)\\s*?\\])[^\\[/\\n]*|/(?!/)[^\\[/\\n]*)*+)\\[assembly:\\s*?%1$s\\s*?\\(\\s*?\\\".*?\\\"\\s*?\\)\\s*?\\]";

        this.assemblyCultureRegex = compile(format(BASE_REGEX, "AssemblyCulture"));
        this.assemblyTrademarkRegex = compile(format(BASE_REGEX, "AssemblyTrademark"));
        this.assemblyCopyrightRegex = compile(format(BASE_REGEX, "AssemblyCopyright"));
        this.assemblyProductRegex = compile(format(BASE_REGEX, "AssemblyProduct"));
        this.assemblyCompanyRegex = compile(format(BASE_REGEX, "AssemblyCompany"));
        this.assemblyDescriptionRegex = compile(format(BASE_REGEX, "AssemblyDescription"));
        this.assemblyTitleRegex = compile(format(BASE_REGEX, "AssemblyTitle"));
        this.assemblyFileVersionRegex = compile(format(BASE_REGEX, "AssemblyFileVersion"));
        this.assemblyInfoVersionRegex = compile(format(BASE_REGEX, "AssemblyInformationalVersion"));
        this.assemblyVersionRegex = compile(format(BASE_REGEX, "AssemblyVersion"));
    }

    @Deprecated
    public ChangeAssemblyVersion(
            String assemblyVersion,
            String assemblyFileVersion,
            String assemblyInformationalVersion,
            String assemblyFile,
            String assemblyTitle,
            String assemblyDescription,
            String assemblyCompany,
            String assemblyProduct,
            String assemblyCopyright,
            String assemblyTrademark,
            String assemblyCulture
    ) {
        this();

        this.assemblyVersion = assemblyVersion;
        this.assemblyFileVersion = assemblyFileVersion;
        this.assemblyInformationalVersion = assemblyInformationalVersion;

        this.assemblyFile = assemblyFile;
        this.assemblyTitle = assemblyTitle;
        this.assemblyDescription = assemblyDescription;
        this.assemblyCompany = assemblyCompany;
        this.assemblyProduct = assemblyProduct;
        this.assemblyCopyright = assemblyCopyright;
        this.assemblyTrademark = assemblyTrademark;
        this.assemblyCulture = assemblyCulture;
        
        /*
        this.assemblyCultureString = "AssemblyCulture";
        this.assemblyTrademarkString = "AssemblyTrademark";
        this.assemblyCopyrightString = "AssemblyCopyright";
        this.assemblyProductString = "AssemblyProduct";
        this.assemblyCompanyString = "AssemblyCompany";
        this.assemblyDescriptionString = "AssemblyDescription";
        this.assemblyTitleString = "AssemblyTitle";
        this.assemblyFileVersionString = "AssemblyFileVersion";
        this.assemblyInfoVersionString = "AssemblyInformationalVersion";
        this.assemblyVersionString = "AssemblyVersion"; */
        
    }

    /**
     * @param assemblyFile the assemblyFile to set
     */
    @DataBoundSetter
    public void setAssemblyFile(String assemblyFile) {
        this.assemblyFile = assemblyFile;
    }

    /**
     * @param assemblyVersion the assemblyVersion to set
     */
    @DataBoundSetter
    public void setAssemblyVersion(String assemblyVersion) {
        this.assemblyVersion = assemblyVersion;
    }

    /**
     * @param assemblyFileVersion the assemblyFileVersion to set
     */
    @DataBoundSetter
    public void setAssemblyFileVersion(String assemblyFileVersion) {
        this.assemblyFileVersion = assemblyFileVersion;
    }

    /**
     * @param assemblyInformationalVersion the assemblyInformationalVersion to
     * set
     */
    @DataBoundSetter
    public void setAssemblyInformationalVersion(String assemblyInformationalVersion) {
        this.assemblyInformationalVersion = assemblyInformationalVersion;
    }

    /**
     * @param assemblyTitle the assemblyTitle to set
     */
    @DataBoundSetter
    public void setAssemblyTitle(String assemblyTitle) {
        this.assemblyTitle = assemblyTitle;
    }

    /**
     * @param assemblyDescription the assemblyDescription to set
     */
    @DataBoundSetter
    public void setAssemblyDescription(String assemblyDescription) {
        this.assemblyDescription = assemblyDescription;
    }

    /**
     * @param assemblyCompany the assemblyCompany to set
     */
    @DataBoundSetter
    public void setAssemblyCompany(String assemblyCompany) {
        this.assemblyCompany = assemblyCompany;
    }

    /**
     * @param assemblyProduct the assemblyProduct to set
     */
    @DataBoundSetter
    public void setAssemblyProduct(String assemblyProduct) {
        this.assemblyProduct = assemblyProduct;
    }

    /**
     * @param assemblyCopyright the assemblyCopyright to set
     */
    @DataBoundSetter
    public void setAssemblyCopyright(String assemblyCopyright) {
        this.assemblyCopyright = assemblyCopyright;
    }

    /**
     * @param assemblyTrademark the assemblyTrademark to set
     */
    @DataBoundSetter
    public void setAssemblyTrademark(String assemblyTrademark) {
        this.assemblyTrademark = assemblyTrademark;
    }

    /**
     * @param assemblyCulture the assemblyCulture to set
     */
    @DataBoundSetter
    public void setAssemblyCulture(String assemblyCulture) {
        this.assemblyCulture = assemblyCulture;
    }

    public String getAssemblyVersion() {
        return this.assemblyVersion;
    }

    public String getAssemblyFileVersion() {
        return this.assemblyFileVersion;
    }

    public String getAssemblyInformationalVersion() {
        return this.assemblyInformationalVersion;
    }

    public String getAssemblyFile() {
        return this.assemblyFile;
    }

    public String getAssemblyTitle() {
        return this.assemblyTitle;
    }

    public String getAssemblyDescription() {
        return this.assemblyDescription;
    }

    public String getAssemblyCompany() {
        return this.assemblyCompany;
    }

    public String getAssemblyProduct() {
        return this.assemblyProduct;
    }

    public String getAssemblyCopyright() {
        return this.assemblyCopyright;
    }

    public String getAssemblyTrademark() {
        return this.assemblyTrademark;
    }

    public String getAssemblyCulture() {
        return this.assemblyCulture;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            try {
                // Expand env variables and token macros
                expandedAssemblyVersion = expandAll(build, listener, this.assemblyVersion);
                expandedAssemblyFileVersion = expandAll(build, listener, this.assemblyFileVersion);
                expandedAssemblyInfoVersion = expandAll(build, listener, this.assemblyInformationalVersion);
                expandedAssemblyTitle = expandAll(build, listener, this.assemblyTitle);
                expandedAssemblyDescription = expandAll(build, listener, this.assemblyDescription);
                expandedAssemblyCompany = expandAll(build, listener, this.assemblyCompany);
                expandedAssemblyProduct = expandAll(build, listener, this.assemblyProduct);
                expandedAssemblyCopyright = expandAll(build, listener, this.assemblyCopyright);
                expandedAssemblyTrademark = expandAll(build, listener, this.assemblyTrademark);
                expandedAssemblyCulture = expandAll(build, listener, this.assemblyCulture);
            } catch (IOException | InterruptedException | MacroEvaluationException ex) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                listener.getLogger().println(sw.toString());

                throw new AbortException(sw.toString());
            }
            perform(build, build.getWorkspace(), launcher, listener);
        } catch (AbortException ex) {
            return false;
        }
        return true;
    }

    /**
     *
     * The perform method is gonna search all the file named "Assemblyinfo.cs"
     * in any folder below, and after found will change the version of
     * AssemblyVersion and AssemblyFileVersion in the file for the inserted
     * version (versionPattern property value).
     *
     *
     * OBS: The inserted value can be some jenkins variable like ${BUILD_NUMBER}
     * just the variable alone, but not implemented to treat
     * 0.0.${BUILD_NUMBER}.0 I think this plugin must be used with Version
     * Number Plugin.
     *
     *
     * @throws hudson.AbortException
     */
    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws AbortException {
        String assemblyGlob = this.assemblyFile == null || this.assemblyFile.isEmpty() ? "**/AssemblyInfo.cs" : this.assemblyFile;

        if (!(build instanceof AbstractBuild)) {    // if in pipleine, nothing was expanded
            expandedAssemblyVersion = assemblyVersion;
            expandedAssemblyFileVersion = this.assemblyFileVersion;
            expandedAssemblyInfoVersion = this.assemblyInformationalVersion;
            expandedAssemblyTitle = this.assemblyTitle;
            expandedAssemblyDescription = this.assemblyDescription;
            expandedAssemblyCompany = this.assemblyCompany;
            expandedAssemblyProduct = this.assemblyProduct;
            expandedAssemblyCopyright =this.assemblyCopyright;
            expandedAssemblyTrademark = this.assemblyTrademark;
            expandedAssemblyCulture = this.assemblyCulture;
        }

        // Log new expanded values
        listener.getLogger().println(format("Changing File(s): %s", assemblyGlob));
        listener.getLogger().println(format("Assembly Version : %s", expandedAssemblyVersion));
        listener.getLogger().println(format("Assembly File Version : %s", expandedAssemblyFileVersion));
        listener.getLogger().println(format("Assembly Informational Version : %s", expandedAssemblyInfoVersion));
        listener.getLogger().println(format("Assembly Title : %s", expandedAssemblyTitle));
        listener.getLogger().println(format("Assembly Description : %s", expandedAssemblyDescription));
        listener.getLogger().println(format("Assembly Company : %s", expandedAssemblyCompany));
        listener.getLogger().println(format("Assembly Product : %s", expandedAssemblyProduct));
        listener.getLogger().println(format("Assembly Copyright : %s", expandedAssemblyCopyright));
        listener.getLogger().println(format("Assembly Trademark : %s", expandedAssemblyTrademark));
        listener.getLogger().println(format("Assembly Culture : %s", expandedAssemblyCulture));

        //FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new AbortException("Unable to retrieve workspace");
        } else {
            try {
                for (FilePath f : workspace.list(assemblyGlob)) {
                    listener.getLogger().println(format("Updating file : %s", f.getRemote()));
                    ByteOrderMark bom;
                    String charset;
                    String content;
                    try (InputStream is = f.read()) {
                        BOMInputStream bs = new BOMInputStream(is); //removes BOM
                        bom = bs.getBOM();    //save the BOM to resinsert later
                        //charset = bs.getBOMCharsetName();
                        charset = bom == null ? Charset.defaultCharset().name() : bom.getCharsetName();
                        content = org.apache.commons.io.IOUtils.toString(bs);
                    }

                    content = replaceOrAppend(content, assemblyVersionRegex, expandedAssemblyVersion, assemblyVersionReplacementString, listener);
                    content = replaceOrAppend(content, assemblyFileVersionRegex, expandedAssemblyFileVersion, assemblyFileVersionReplacementString, listener);
                    content = replaceOrAppend(content, assemblyInfoVersionRegex, expandedAssemblyInfoVersion, assemblyInfoVersionReplacementString, listener);
                    content = replaceOrAppend(content, assemblyTitleRegex, expandedAssemblyTitle, assemblyTitleReplacmentString, listener);
                    content = replaceOrAppend(content, assemblyDescriptionRegex, expandedAssemblyDescription, assemblyDescriptionReplacementString, listener);
                    content = replaceOrAppend(content, assemblyCompanyRegex, expandedAssemblyCompany, assemblyCompanyReplacementString, listener);
                    content = replaceOrAppend(content, assemblyProductRegex, expandedAssemblyProduct, assemblyProductReplacmentString, listener);
                    content = replaceOrAppend(content, assemblyCopyrightRegex, expandedAssemblyCopyright, assemblyCopyrightReplacementString, listener);
                    content = replaceOrAppend(content, assemblyTrademarkRegex, expandedAssemblyTrademark, assemblyTrademarkReplacementString, listener);
                    content = replaceOrAppend(content, assemblyCultureRegex, expandedAssemblyCulture, assemblyCultureReplacementString, listener);
                    
                    try (OutputStream out = f.write()) {
                        if (bom != null){
                            out.write(bom.getBytes());
                        }
                        out.write(content.getBytes(charset));
                        out.flush();
                    }
                    catch (Exception ex){
                        listener.getLogger().println(ex.getMessage());
                    }
                }
            } catch (IOException | InterruptedException ex) {
                throw new AbortException(ex.getMessage());
            }
        }
    }

    @Extension
    @Symbol("changeAsmVer")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Change Assembly Version";
        }
    }
}
