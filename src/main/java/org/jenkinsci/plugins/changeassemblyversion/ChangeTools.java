package org.jenkinsci.plugins.changeassemblyversion;

import hudson.model.TaskListener;
import java.io.IOException;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeTools {

    ChangeTools() {
    }

    public static String replaceOrAppend(String content, Pattern regexPattern, String replacement, String replacementPattern, TaskListener listener) throws IOException, InterruptedException {
        if (replacement != null && !replacement.isEmpty()) {
            //listener.getLogger().println(String.format("\t Replacement : %s", replacement));
            //String newContent = content.replaceAll(regexPattern.toString(), String.format(replacementPattern, replacement));
            //regexPattern.matcher(content).region(0, 0);
            Matcher m=regexPattern.matcher(content);
            content = m.replaceFirst(format(replacementPattern, replacement));
            //listener.getLogger().println(String.format("regex= %s",regexPattern.matcher(content).pattern()));
            try {
                m.group(); //throws illegalstate if no match was perfomred
            } catch (IllegalStateException ex) {
                String s=String.format(replacementPattern, replacement);
                listener.getLogger().println("Addidng missing value "+s);
                content += lineSeparator() + s;
            }
        } else {
            //listener.getLogger().println(String.format("Skipping replacement because replacemnt value is empty."));
        }
        return content;
    }
}
