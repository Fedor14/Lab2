package aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(void controller.Editor.*(..)) && args(actionCommand)")
    public void logEditorActions(String actionCommand) {
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