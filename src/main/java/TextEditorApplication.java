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
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        EditorUI editorUI1 = context.getBean(EditorUI.class);
        EditorUI editorUI2 = context.getBean(EditorUI.class);
        UndoManager undoManager = context.getBean(UndoManager.class);
        LoggingAspect loggingAspect = context.getBean(LoggingAspect.class); // Добавлено

        Editor editor1 = new Editor(editorUI1, undoManager, loggingAspect); // Изменено
        Editor editor2 = new Editor(editorUI2, undoManager, loggingAspect); // Изменено

        TextEditorObserver observer1 = new TextEditorObserver(editor2);
        TextEditorObserver observer2 = new TextEditorObserver(editor1);

        editor1.addObserver(observer1);
        editor2.addObserver(observer2);
    }
}


