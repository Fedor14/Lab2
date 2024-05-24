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

@Configuration // Объявление класса как конфигурационного для Spring контекста
@EnableAspectJAutoProxy // Включение поддержки аспектов в приложении
@ComponentScan({"controller", "aspect", "ui", "observer"}) // Сканирование компонентов в указанных пакетах

public class AppConfig {

    @Bean
    @Scope("prototype") // Указание области видимости бина как prototype
    public EditorUI editorUI() {
        return new EditorUI(); // Создание и возвращение нового экземпляра EditorUI
    }

    @Bean
    @Scope("prototype") // Указание области видимости бина как prototype
    public LoggingAspect loggingAspect() {
        return new LoggingAspect(); // Создание и возвращение нового экземпляра LoggingAspect
    }

    @Bean
    @Scope("prototype") // Указание области видимости бина как prototype
    public Editor editor(EditorUI editorUI, UndoManager undoManager, LoggingAspect loggingAspect) {
        return new Editor(editorUI, undoManager, loggingAspect); // Создание и возвращение нового экземпляра Editor
    }

    @Bean
    @Scope("singleton") // Указание области видимости бина как singleton
    public UndoManager undoManager() {
        return new UndoManager(); // Создание и возвращение нового экземпляра UndoManager
    }
}





