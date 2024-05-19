package ui;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import org.springframework.stereotype.Component;

@Component
public class EditorUI {
    // Основное окно
    private static JFrame frame;

    public EditorUI() {
        // Создание окна
        frame = new JFrame("Editor");

        try {
            // Установка MetalLookAndFeel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // Установка темы OceanTheme
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JFrame getFrame() {
        return frame;
    }

    public static void display() {
        frame.setSize(500, 500);
        frame.setVisible(true);
    }
}
