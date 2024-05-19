package aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* controller.EditorUI.*(..))")
    public void editorUIActions() {}

    @Pointcut("execution(* controller.Editor.*(..))")
    public void editorActions() {}

    @AfterReturning("editorUIActions() && args(actionCommand)")
    public void logUIActions(String actionCommand) {
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

    @AfterReturning("editorActions() && args(actionCommand)")
    public void logEditorActions(String actionCommand) {
        switch (actionCommand) {
            default:
                break;
        }
    }
}