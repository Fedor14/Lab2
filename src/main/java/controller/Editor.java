package controller;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.event.*;
import java.io.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ui.EditorUI;

// Объявление класса как компонента Spring
@Component
public class Editor implements ActionListener {

    private JTextArea textArea;
    private JFrame frame;
    private File openedFile = null; // Хранит открытый файл, если он есть

    private UndoManager undoManager;

    // Автоматическая инъекции зависимостей
    @Autowired
    public Editor() {
        this.frame = EditorUI.getFrame();   // // Получение фрейма из пользовательского интерфейса
        this.textArea = new JTextArea();
        this.undoManager = new UndoManager();

        // Создание меню
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Correction");

        // Создание пунктов меню
        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem saveAsMenuItem = new JMenuItem("Save as");
        JMenuItem closeMenuItem = new JMenuItem("Close");

        JMenuItem forwardMenuItem = new JMenuItem("Previous");
        JMenuItem backMenuItem = new JMenuItem("Following");

        // Установка команд действий для пунктов меню
        setActionCommands(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);

        // Добавление обработчиков действий к пунктам меню
        addActionListeners(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);

        // Добавление пунктов меню в меню
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        editMenu.add(forwardMenuItem);
        editMenu.add(backMenuItem);

        // Добавление пунктов меню в меню
        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        // Установка меню бара для фрейма
        frame.setJMenuBar(menuBar);
        frame.add(textArea);

        // Добавление слушателя отмены изменений в текстовое поле
        textArea.getDocument().addUndoableEditListener(undoManager);

        // Пользовательский интерфейс
        EditorUI.display();
    }

    // Установка команд действий для пунктов меню
    private void setActionCommands(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.setActionCommand(item.getText());
        }
    }

    // Обработчики действий
    private void addActionListeners(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.addActionListener(this);
        }
    }

    // Выполнение действия
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        // Цепочка обработчиков действий
        ActionHandler handlerChain = new NewFileHandler(new OpenFileHandler(new SaveFileHandler(new SaveAsFileHandler(new CloseHandler(new ForwardHandler(new BackHandler(null)))))));
        handlerChain.handleRequest(actionCommand);
    }

    // Интерфейс обработчика действий
    interface ActionHandler {
        void handleRequest(String actionCommand);
    }

    // Обработчик для создания нового файла
    class NewFileHandler implements ActionHandler {
        private ActionHandler next;

        NewFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("New")) {
                textArea.setText("");
                openedFile = null;
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    // Обработчик для открытия файла
    class OpenFileHandler implements ActionHandler {
        private ActionHandler next;

        OpenFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Open")) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    openedFile = selectedFile;

                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            textArea.append(line + "\n");
                        }
                        reader.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    // Обработчик для сохранения файла
    class SaveFileHandler implements ActionHandler {
        private ActionHandler next;

        SaveFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Save")) {
                saveFile();
            } else {
                next.handleRequest(actionCommand);
            }
        }

        private void saveFile() {
            if (openedFile != null) {
                try {
                    FileWriter writer = new FileWriter(openedFile);
                    writer.write(textArea.getText());
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    openedFile = selectedFile;

                    try {
                        FileWriter writer = new FileWriter(selectedFile);
                        writer.write(textArea.getText());
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    // Обработчик для сохранения файла под новым именем
    class SaveAsFileHandler implements ActionHandler {
        private ActionHandler next;

        SaveAsFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Save as")) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    openedFile = selectedFile;

                    try {
                        FileWriter writer = new FileWriter(selectedFile);
                        writer.write(textArea.getText());
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    // Обработчик для закрытия редактора
    class CloseHandler implements ActionHandler {
        private ActionHandler next;

        CloseHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Close")) {
                frame.dispose();
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    // Мотаем назад
    class ForwardHandler implements ActionHandler {
        private ActionHandler next;

        ForwardHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Previous")) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    // Мотаем вперёд
    class BackHandler implements ActionHandler {
        private ActionHandler next;

        BackHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Following")) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }
}