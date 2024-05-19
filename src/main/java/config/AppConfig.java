package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import controller.Editor;
import ui.EditorUI;
import javax.swing.undo.UndoManager;

// Объявление класса как конфигурационного
@Configuration
public class AppConfig {

    // Определение бина Editor
    @Bean
    @Scope("singleton")
    public Editor editor() {
        return new Editor();
    }

    // Определение бина EditorUI
    @Bean
    @Scope("singleton")
    public EditorUI editorUI() {
        return new EditorUI();
    }

    // Определение бина UndoManager
    @Bean
    @Scope("prototype")
    public UndoManager undoManager() {
        return new UndoManager();
    }
}
