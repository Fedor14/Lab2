import aspect.LoggingAspect;
import config.AppConfig;
import controller.Editor;
import observer.TextEditorObserver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ui.EditorUI;
import javax.swing.undo.UndoManager;

public class TextEditorApplication {

    public static void main(String[] args) {

        // Создание контекста на основе конфигурации AppConfig
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Получение бинов из контекста
        EditorUI editorUI1 = context.getBean(EditorUI.class);
        EditorUI editorUI2 = context.getBean(EditorUI.class);
        UndoManager undoManager = context.getBean(UndoManager.class); // Получение экземпляра UndoManager из контекста
        LoggingAspect loggingAspect = context.getBean(LoggingAspect.class); // Получение экземпляра LoggingAspect из контекста

        // Создание и конфигурирование редакторов с использованием полученных бинов
        Editor editor1 = new Editor(editorUI1, undoManager, loggingAspect);
        Editor editor2 = new Editor(editorUI2, undoManager, loggingAspect);

        // Создание и регистрация наблюдателей для обновления редакторов
        TextEditorObserver observer1 = new TextEditorObserver(editor2);
        TextEditorObserver observer2 = new TextEditorObserver(editor1);

        // Регистрация наблюдателей в редакторах
        editor1.addObserver(observer1);
        editor2.addObserver(observer2);
    }
}



