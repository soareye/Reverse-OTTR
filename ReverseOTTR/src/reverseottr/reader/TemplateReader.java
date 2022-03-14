package reverseottr.reader;

import java.util.LinkedList;
import java.util.List;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.model.Template;


public class TemplateReader {

    /**
     *
     * @param fileName
     * @return List of templates found in file
     */
    public List<Template> read(String fileName) {
        List<Template> list = new LinkedList<>();
        TemplateManager templateManager = new StandardTemplateManager();
        templateManager.readLibrary(fileName);
        templateManager.getTemplateStore().getAllTemplates().innerForEach(list::add);

        return list;
    }
}
