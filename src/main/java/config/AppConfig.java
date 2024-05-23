package config;

import aspect.LoggingAspect;
import controller.Editor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import javax.swing.undo.UndoManager;
import ui.EditorUI;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan({"controller", "aspect", "ui", "observer"})
public class AppConfig {

    @Bean
    @Scope("prototype")
    public EditorUI editorUI() {
        return new EditorUI();
    }

    @Bean
    @Scope("prototype")
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    @Scope("prototype")
    public Editor editor(EditorUI editorUI, UndoManager undoManager, LoggingAspect loggingAspect) {
        return new Editor(editorUI, undoManager, loggingAspect);
    }

    @Bean
    @Scope("singleton")
    public UndoManager undoManager() {
        return new UndoManager();
    }
}




