import controller.Editor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TextEditorApplication {

    public static void main(String[] args) {

        // Создание контекста Spring и загрузка конфигурации из XML-файла "applicationContext.xml"
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        // Получение бина класса Editor
        Editor editor = context.getBean(Editor.class);
    }
}