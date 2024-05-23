package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import javax.swing.undo.UndoManager;
import controller.Editor;
import ui.EditorUI;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan({"controller", "aspect", "ui"})
public class AppConfig {

    @Bean
    @Scope("prototype")
    public EditorUI editorUI() {
        return new EditorUI();
    }

    @Bean
    @Scope("prototype")
    public Editor editor(EditorUI editorUI, UndoManager undoManager) {
        return new Editor(editorUI, undoManager);
    }

    @Bean
    @Scope("singleton")
    public UndoManager undoManager() {
        return new UndoManager();
    }
}
