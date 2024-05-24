package ui;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import org.springframework.stereotype.Component;

@Component
public class EditorUI {
    // Основное окно
    private JFrame frame; // Приватное поле для хранения ссылки на JFrame, представляющий основное окно редактора

    public EditorUI() { // Конструктор класса
        // Создание окна
        frame = new JFrame("Editor");

        try {
            // Установка MetalLookAndFeel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // Установка темы OceanTheme
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } catch (Exception e) {
            e.printStackTrace(); // Вывод стека вызовов и информации об исключении
        }
    }

    public JFrame getFrame() { // Метод для получения ссылки на основное окно редактора
        return frame; // Возвращает ссылку на JFrame
    }

    public void display() { // Метод для отображения окна редактора
        frame.setSize(500, 500);
        frame.setVisible(true); // Делает окно видимым
    }
}
