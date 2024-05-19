package aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect // Объявление класса LoggingAspect как аспекта
@Component
public class LoggingAspect {

    // Выполняется перед вызовом метода actionPerformed в классе controller.Editor
    @Before("execution(* controller.Editor.actionPerformed(..))")
    public void logBeforeAction() {
        System.out.println("Действие пользователя перехвачено.");
    }

    // Выполняется перед вызовом метода display в классе ui.EditorUI
    @Before("execution(* ui.EditorUI.display(..))")
    public void logBeforeDisplay() {
        System.out.println("Пользовательский интерфейс отображается.");
    }
}
