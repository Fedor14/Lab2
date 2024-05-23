import config.AppConfig;
import controller.Editor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TextEditorApplication {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        Editor editor1 = context.getBean(Editor.class);
        Editor editor2 = context.getBean(Editor.class);
    }
}

