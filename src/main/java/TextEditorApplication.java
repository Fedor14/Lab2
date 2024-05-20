import config.AppConfig;
import controller.Editor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TextEditorApplication {
    public static void main(String[] args) {
        // Использование конфигурации на основе Java
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Editor editor = context.getBean(Editor.class);
    }
}