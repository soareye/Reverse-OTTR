package reverseottr.reader;

import xyz.ottr.lutra.api.StandardFormat;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.io.Format;
import xyz.ottr.lutra.model.Template;
import xyz.ottr.lutra.stottr.util.SSyntaxChecker;
import xyz.ottr.lutra.system.MessageHandler;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class RTemplateReader {

    /**
     * Check stottr syntax and file existence.
     * @param path
     */
    public static void check(String path) {
        MessageHandler messageHandler = new MessageHandler(System.out);
        SSyntaxChecker syntaxChecker = new SSyntaxChecker(messageHandler);

        try {
            syntaxChecker.checkFile(Paths.get(path));
            messageHandler.printMessages();

        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public static List<Template> read(String formatName, String path) {
        StandardTemplateManager templateManager = new StandardTemplateManager();

        Format format = templateManager.getFormat(formatName);

        List<Template> result = new LinkedList<>();
        templateManager.getTemplateStore().getAllTemplates().innerForEach(result::add);

        return result;
    }
}
