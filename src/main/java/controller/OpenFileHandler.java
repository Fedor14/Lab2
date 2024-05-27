package controller;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// Обработчик для команды "Open"
class OpenFileHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    OpenFileHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Open")) {
            JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
            int returnValue = fileChooser.showOpenDialog(editor.frame);  // Отображение диалогового окна

            if (returnValue == JFileChooser.APPROVE_OPTION) {  // Проверка, был ли файл выбран
                File selectedFile = fileChooser.getSelectedFile();  // Получение выбранного файла

                // Чтение файла в отдельном потоке
                new Thread(() -> {
                    try {
                        editor.readWriteLock.writeLock();
                        editor.openedFile = selectedFile;  // Установка открытого файла

                        BufferedReader reader = new BufferedReader(new FileReader(selectedFile));  // BufferedReader для чтения файла
                        StringBuilder content = new StringBuilder();  //StringBuilder для накопления содержимого файла
                        String line;
                        while ((line = reader.readLine()) != null) {  // Чтение файла построчно
                            content.append(line).append("\n");  // Добавление строки в StringBuilder
                        }
                        reader.close();  // Закрытие файла

                        // Обновление текстовой области в главном потоке
                        SwingUtilities.invokeLater(() -> editor.textArea.setText(content.toString()));  // Установка текста в текстовую область
                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace();
                    } finally {
                        editor.readWriteLock.writeUnlock();
                    }
                }).start();
            }
        } else {
            next.handleRequest(actionCommand);
        }
    }
}
