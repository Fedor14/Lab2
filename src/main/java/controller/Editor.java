package controller;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import aspect.LoggingAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ui.EditorUI;

import observer.EditorObserver;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

@Component // Указывает, что этот класс является Spring-компонентом
public class Editor implements ActionListener {

    private JTextArea textArea; // Текстовое поле для ввода текста
    private JFrame frame; // Основное окно приложения
    private File openedFile = null; // Ссылка на открытый файл

    private UndoManager undoManager; // Менеджер отмены и повтора действий

    private List<EditorObserver> observers = new ArrayList<>(); // Список наблюдателей

    private boolean isUpdating = false; // Флаг, указывающий, происходит ли обновление текста

    private String previousState; // Предыдущее состояние текста для функции "Following"

    @Autowired // Автоматическое внедрение зависимости
    private LoggingAspect loggingAspect;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // Замок для обеспечения потокобезопасности
    private final Lock readLock = rwLock.readLock(); // Замок для чтения
    private final Lock writeLock = rwLock.writeLock(); // Замок для записи

    @Autowired
    public Editor(EditorUI editorUI, UndoManager undoManager, LoggingAspect loggingAspect) {
        this.frame = editorUI.getFrame(); // Получение окна от EditorUI
        this.textArea = new JTextArea(); // Создание текстового поля
        this.undoManager = undoManager; // Инициализация UndoManager
        this.loggingAspect = loggingAspect; // Инициализация LoggingAspect

        // Создание меню
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Correction");

        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem saveAsMenuItem = new JMenuItem("Save as");
        JMenuItem closeMenuItem = new JMenuItem("Close");

        JMenuItem forwardMenuItem = new JMenuItem("Previous");
        JMenuItem backMenuItem = new JMenuItem("Following");

        setActionCommands(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem); // Установка команд действий для пунктов меню
        addActionListeners(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem); // Добавление обработчиков событий для пунктов меню

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        editMenu.add(forwardMenuItem);
        editMenu.add(backMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        frame.setJMenuBar(menuBar); // Установка панели меню для окна
        frame.add(textArea); // Добавление текстового поля в окно

        textArea.getDocument().addUndoableEditListener(undoManager); // Добавление слушателя изменений текста для UndoManager
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers(); // Уведомление наблюдателей при вставке текста
                }
            }

            public void removeUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers(); // Уведомление наблюдателей при удалении текста
                }
            }

            public void changedUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers(); // Уведомление наблюдателей при изменении текста
                }
            }
        });
        editorUI.display(); // Отображение окна
    }

    public void addObserver(EditorObserver observer) {
        writeLock.lock(); // Захват замка для записи
        try {
            observers.add(observer); // Добавление наблюдателя
        } finally {
            writeLock.unlock(); // Освобождение замка
        }
    }

    private void notifyObservers() {
        readLock.lock(); // Захват замка для чтения
        try {
            String text = textArea.getText(); // Получение текста из текстового поля
            for (EditorObserver observer : observers) {
                observer.update(text); // Уведомление наблюдателя
            }
        } finally {
            readLock.unlock(); // Освобождение замка
        }
    }

    public void setText(String text) {
        writeLock.lock(); // Захват замка для записи
        try {
            isUpdating = true; // Установка флага, что происходит обновление текста
            try {
                textArea.setText(text); // Установка текста в текстовое поле
            } finally {
                isUpdating = false; // Сброс флага
            }
        } finally {
            writeLock.unlock(); // Освобождение замка
        }
    }

    private void setActionCommands(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.setActionCommand(item.getText()); // Установка команды действия для каждого пункта меню
        }
    }

    private void addActionListeners(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.addActionListener(this); // Добавление обработчика событий для каждого пункта меню
        }
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand(); // Получение команды действия

        // Цепочка обязанностей для обработки команд действий
        ActionHandler handlerChain = new NewFileHandler(new OpenFileHandler(new SaveFileHandler(new SaveAsFileHandler(new CloseHandler(new ForwardHandler(new BackHandler(null)))))));
        handlerChain.handleRequest(actionCommand); // Обработка команды через цепочку обязанностей

        this.loggingAspect.logEditorActions(actionCommand); // Вызов метода логирования действия

        System.out.println("Выполнено действие: " + actionCommand);
    }

    interface ActionHandler {
        void handleRequest(String actionCommand); // Интерфейс для обработчиков команд
    }

    class NewFileHandler implements ActionHandler {
        private ActionHandler next;

        NewFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("New")) {
                writeLock.lock(); // Захват замка для записи
                try {
                    textArea.setText(""); // Очистка текстового поля
                    openedFile = null; // Сброс ссылки на открытый файл
                } finally {
                    writeLock.unlock(); // Освобождение замка
                }
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }
    }

    class OpenFileHandler implements ActionHandler {
        private ActionHandler next;

        OpenFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Open")) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(frame); // Открытие диалогового окна для выбора файла

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile(); // Получение выбранного файла
                    writeLock.lock(); // Захват замка для записи
                    try {
                        openedFile = selectedFile;

                        BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n"); // Чтение содержимого файла
                        }
                        reader.close();

                        textArea.setText(content.toString()); // Установка содержимого файла в текстовое поле
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        writeLock.unlock(); // Освобождение замка
                    }
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }

    class SaveFileHandler implements ActionHandler {
        private ActionHandler next;

        SaveFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Save")) {
                saveFile(); // Вызов метода сохранения файла
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }

        private void saveFile() {
            writeLock.lock(); // Захват замка для записи
            try {
                if (openedFile != null) {
                    try (FileWriter writer = new FileWriter(openedFile)) {
                        writer.write(textArea.getText()); // Запись содержимого текстового поля в файл
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JFileChooser fileChooser = new JFileChooser();
                    int returnValue = fileChooser.showSaveDialog(frame); // Открытие диалогового окна для сохранения файла

                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        openedFile = selectedFile;

                        try (FileWriter writer = new FileWriter(selectedFile)) {
                            writer.write(textArea.getText()); // Запись содержимого текстового поля в файл
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } finally {
                writeLock.unlock(); // Освобождение замка
            }
        }
    }

    class SaveAsFileHandler implements ActionHandler {
        private ActionHandler next;

        SaveAsFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Save as")) {
                writeLock.lock(); // Захват замка для записи
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    int returnValue = fileChooser.showSaveDialog(frame); // Открытие диалогового окна для сохранения файла

                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        openedFile = selectedFile;

                        try (FileWriter writer = new FileWriter(selectedFile)) {
                            writer.write(textArea.getText()); // Запись содержимого текстового поля в файл
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                } finally {
                    writeLock.unlock(); // Освобождение замка
                }
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }
    }

    class CloseHandler implements ActionHandler {
        private ActionHandler next;

        CloseHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Close")) {
                writeLock.lock(); // Захват замка для записи
                try {
                    frame.dispose(); // Закрытие окна
                } finally {
                    writeLock.unlock(); // Освобождение замка
                }
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }
    }

    class ForwardHandler implements ActionHandler {
        private ActionHandler next;

        ForwardHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Previous")) {
                if (undoManager.canUndo()) {
                    previousState = textArea.getText(); // Сохранение текущего состояния текста перед отменой
                    undoManager.undo(); // Отмена последнего действия
                }
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }
    }

    class BackHandler implements ActionHandler {
        private ActionHandler next;

        BackHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Following")) {
                if (previousState != null) {
                    textArea.setText(previousState); // Восстановление сохраненного состояния текста
                }
            } else {
                next.handleRequest(actionCommand); // Передача команды следующему обработчику
            }
        }
    }
}