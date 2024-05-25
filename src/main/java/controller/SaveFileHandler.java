package controller;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class SaveFileHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    SaveFileHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Save")) {
            new Thread(() -> {  // Создание нового потока для обработки команды сохранения файла
                try {
                    editor.acquireLock();
                    saveFile();  // Вызов метода сохранения файла
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

    private void saveFile() {
        if (editor.openedFile != null) {  // Проверка, открыт ли файл
            try {
                FileWriter writer = new FileWriter(editor.openedFile);  // Создание FileWriter для записи в файл
                writer.write(editor.textArea.getText());  // Запись содержимого текстовой области в файл
                writer.close();  // Закрытие файла
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
            int returnValue = fileChooser.showSaveDialog(editor.frame);  // Отображение диалогового окна

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
        }
    }
}
