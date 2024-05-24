package aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    // Определение среза, который будет срабатывать перед выполнением любого метода в классе Editor
    @Before("execution(void controller.Editor.*(..)) && args(actionCommand)")
    public void logEditorActions(String actionCommand) {
        // Метод, который будет выполняться перед целевым методом, указанный в срезе

        switch (actionCommand) {
            case "Open":
                System.out.println("Файл открыт");
                break;
            case "Save":
                System.out.println("Файл сохранён");
                break;
            default:
                break;
        }
    }
}
