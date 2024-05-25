package controller;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class SaveAsFileHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    SaveAsFileHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Save as")) {
            new Thread(() -> {  // Создание нового потока
                try {
                    editor.acquireLock();
                    JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
                    int returnValue = fileChooser.showSaveDialog(editor.frame);  // Отображение диалогового окна и получение результата

                    if (returnValue == JFileChooser.APPROVE_OPTION) {  // Проверка, был ли файл выбран
                        File selectedFile = fileChooser.getSelectedFile();  // Получение выбранного файла
                        editor.openedFile = selectedFile;  // Установка открытого файла

                        try {
                            FileWriter writer = new FileWriter(selectedFile);  // FileWriter для записи в файл
                            writer.write(editor.textArea.getText());  // Запись содержимого текстовой области в файл
                            writer.close();  // Закрытие файла
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    editor.releaseLock();
                }
            }).start();
        } else {
            next.handleRequest(actionCommand);
        }
    }
}
