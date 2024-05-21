package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import javax.swing.undo.UndoManager;
import controller.Editor;
import ui.EditorUI;

@Configuration
@ComponentScan({"controller", "aspect"})
public class AppConfig {

    @Bean
    @Scope("singleton")
    public EditorUI editorUI() {
        return new EditorUI();
    }

    @Bean
    @Scope("singleton")
    public Editor editor(EditorUI editorUI, UndoManager undoManager) {
        return new Editor(editorUI(), undoManager()); // Внедрение EditorUI и UndoManager через конструктор
    }

    @Bean
    @Scope("prototype")
    public UndoManager undoManager() {
        return new UndoManager();
    }
}
