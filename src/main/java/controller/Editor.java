package controller;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.io.*;

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

@Component
public class Editor implements ActionListener {

    private JTextArea textArea;  // Текстовая область для ввода текста
    private JFrame frame;  // Основное окно приложения
    private File openedFile = null;  // Текущий открытый файл, если есть

    private UndoManager undoManager;  // Менеджер отмены действий для текстовой области

    private List<EditorObserver> observers = new ArrayList<>();  // Список наблюдателей, которые будут уведомлены об изменениях текста

    private boolean isUpdating = false;  // Флаг для предотвращения рекурсивных вызовов при обновлении текста

    private String previousState;  // Предыдущее состояние текста, используется для функции отмены

    @Autowired
    private LoggingAspect loggingAspect;  // Логирования действий

    private final SimpleMutex mutex = new SimpleMutex();  // Мьютекс для синхронизации доступа

    private void acquireLock() throws InterruptedException {  // Метод для получения блокировки
        mutex.lock();
    }

    private void releaseLock() {  // Метод для освобождения блокировки
        mutex.unlock();
    }

    @Autowired
    public Editor(EditorUI editorUI, UndoManager undoManager, LoggingAspect loggingAspect) {
        this.frame = editorUI.getFrame();  // Инициализация окна
        this.textArea = new JTextArea();  // Создание текстовой области
        this.undoManager = undoManager;  // Инициализация менеджера отмены
        this.loggingAspect = loggingAspect;  // Инициализация аспекта логирования

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

        // Установка команд действий для каждого пункта меню
        setActionCommands(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);

        // Добавление слушателей действий для каждого пункта меню
        addActionListeners(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        editMenu.add(forwardMenuItem);
        editMenu.add(backMenuItem);

        menuBar.add(fileMenu);  // Добавление меню "File" в меню-бар
        menuBar.add(editMenu);  // Добавление меню "Correction" в меню-бар

        frame.setJMenuBar(menuBar);  // Установка меню-бара в окне
        frame.add(textArea);  // Добавление текстовой области в окно

        textArea.getDocument().addUndoableEditListener(undoManager);  // Добавление слушателя изменений для менеджера отмены

        // Добавление слушателя изменений документа
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {  // Метод вызывается при вставке текста
                if (!isUpdating) {
                    notifyObservers();  // Уведомление наблюдателей об изменении текста
                }
            }

            public void removeUpdate(DocumentEvent e) {  // Метод вызывается при удалении текста
                if (!isUpdating) {
                    notifyObservers();  // Уведомление наблюдателей об изменении текста
                }
            }

            public void changedUpdate(DocumentEvent e) {  // Метод вызывается при изменении атрибутов текста
                if (!isUpdating) {
                    notifyObservers();  // Уведомление наблюдателей об изменении текста
                }
            }
        });
        editorUI.display();
    }

    // Добавляет нового наблюдателя
    public void addObserver(EditorObserver observer) {
        try {
            acquireLock();  // Захват блокировки
            observers.add(observer);  // Добавление нового наблюдателя в список
        } catch (InterruptedException e) {  // Обработка исключения при возникновении ошибки во время захвата блокировки
            e.printStackTrace();  // Вывод стека вызовов для отладки
        } finally {
            releaseLock();  // Освобождение блокировки
        }
    }


    // Уведомляет всех наблюдателей о текущем тексте в текстовой области.
    private void notifyObservers() {
        try {
            acquireLock();
            String text = textArea.getText();  // Получение текста из текстовой области
            for (EditorObserver observer : observers) {  // Перебор всех наблюдателей
                observer.update(text);  // Уведомление каждого наблюдателя о новом тексте
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    // Устанавливает новый текст в текстовой области.
    public void setText(String text) {
        try {
            acquireLock();
            isUpdating = true;  // Установка флага обновления текста
            try {
                textArea.setText(text);  // Установка нового текста в текстовую область
            } finally {
                isUpdating = false;  // Сброс флага обновления текста после установки текста
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    // Метод для установки команд действий для пунктов меню
    private void setActionCommands(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.setActionCommand(item.getText());
        }
    }

    // Метод для добавления слушателей действий для пунктов меню
    private void addActionListeners(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.addActionListener(this);
        }
    }

    // Метод, вызываемый при выполнении действия (нажатии пункта меню)
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();  // Получение команды действия

        // Создание цепочки ответственности для обработки команд действий
        ActionHandler handlerChain = new NewFileHandler(new OpenFileHandler(new SaveFileHandler(new SaveAsFileHandler(new CloseHandler(new ForwardHandler(new BackHandler(null)))))));
        handlerChain.handleRequest(actionCommand);  // Обработка команды действия

        // Вызов метода логирования действий через внедренный аспект
        this.loggingAspect.logEditorActions(actionCommand);

        System.out.println("Action performed: " + actionCommand);
    }

    interface ActionHandler {  // Интерфейс для обработчиков команд действий
        void handleRequest(String actionCommand);
    }

    // Обработчик для команды "New"
    class NewFileHandler implements ActionHandler {
        private ActionHandler next;  // Ссылка на следующий обработчик в цепочке

        NewFileHandler(ActionHandler next) {
            this.next = next;  // Инициализация следующего обработчика
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("New")) {
                new Thread(() -> {  // Создание нового потока для обработки команды
                    try {
                        acquireLock();  // Захват блокировки
                        textArea.setText("");  // Очистка текстовой области
                        openedFile = null;  // Сброс открытого файла
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();  // Обработка исключения
                    } finally {
                        releaseLock();  // Освобождение блокировки
                    }
                }).start();  // Запуск потока
            } else {
                next.handleRequest(actionCommand);  // Передача команды следующему обработчику в цепочке
            }
        }
    }

    // Обработчик для команды "Open"
    class OpenFileHandler implements ActionHandler {
        private ActionHandler next;

        OpenFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Open")) {
                JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
                int returnValue = fileChooser.showOpenDialog(frame);  // Отображение диалогового окна

                if (returnValue == JFileChooser.APPROVE_OPTION) {  // Проверка, был ли файл выбран
                    File selectedFile = fileChooser.getSelectedFile();  // Получение выбранного файла

                    // Чтение файла в отдельном потоке
                    new Thread(() -> {
                        try {
                            acquireLock();
                            openedFile = selectedFile;  // Установка открытого файла

                            BufferedReader reader = new BufferedReader(new FileReader(selectedFile));  // BufferedReader для чтения файла
                            StringBuilder content = new StringBuilder();  //StringBuilder для накопления содержимого файла
                            String line;
                            while ((line = reader.readLine()) != null) {  // Чтение файла построчно
                                content.append(line).append("\n");  // Добавление строки в StringBuilder
                            }
                            reader.close();  // Закрытие файла

                            // Обновление текстовой области в главном потоке
                            SwingUtilities.invokeLater(() -> textArea.setText(content.toString()));  // Установка текста в текстовую область
                        } catch (IOException | InterruptedException ex) {
                            ex.printStackTrace();
                        } finally {
                            releaseLock();
                        }
                    }).start();
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
                new Thread(() -> {  // Создание нового потока для обработки команды сохранения файла
                    try {
                        acquireLock();
                        saveFile();  // Вызов метода сохранения файла
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } finally {
                        releaseLock();
                    }
                }).start();
            } else {
                next.handleRequest(actionCommand);
            }
        }

        private void saveFile() {
            if (openedFile != null) {  // Проверка, открыт ли файл
                try {
                    FileWriter writer = new FileWriter(openedFile);  // Создание FileWriter для записи в файл
                    writer.write(textArea.getText());  // Запись содержимого текстовой области в файл
                    writer.close();  // Закрытие файла
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
                int returnValue = fileChooser.showSaveDialog(frame);  // Отображение диалогового окна

                if (returnValue == JFileChooser.APPROVE_OPTION) {  // Проверка, был ли файл выбран
                    File selectedFile = fileChooser.getSelectedFile();  // Получение выбранного файла
                    openedFile = selectedFile;  // Установка открытого файла

                    try {
                        FileWriter writer = new FileWriter(selectedFile);  // FileWriter для записи в файл
                        writer.write(textArea.getText());  // Запись содержимого текстовой области в файл
                        writer.close();  // Закрытие файла
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
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
                new Thread(() -> {  // Создание нового потока
                    try {
                        acquireLock();
                        JFileChooser fileChooser = new JFileChooser();  // Создание диалогового окна
                        int returnValue = fileChooser.showSaveDialog(frame);  // Отображение диалогового окна и получение результата

                        if (returnValue == JFileChooser.APPROVE_OPTION) {  // Проверка, был ли файл выбран
                            File selectedFile = fileChooser.getSelectedFile();  // Получение выбранного файла
                            openedFile = selectedFile;  // Установка открытого файла

                            try {
                                FileWriter writer = new FileWriter(selectedFile);  // FileWriter для записи в файл
                                writer.write(textArea.getText());  // Запись содержимого текстовой области в файл
                                writer.close();  // Закрытие файла
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } finally {
                        releaseLock();
                    }
                }).start();
            } else {
                next.handleRequest(actionCommand);
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
                new Thread(() -> {  // Создание нового потока
                    try {
                        acquireLock();
                        frame.dispose();  // Закрытие окна приложения
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } finally {
                        releaseLock();
                    }
                }).start();
            } else {
                next.handleRequest(actionCommand);
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
                previousState = textArea.getText();  // Сохранение предыдущего состояния текстовой области
                SwingUtilities.invokeLater(() -> undoManager.undo());  // Выполнение операции отмены
            } else {
                next.handleRequest(actionCommand);
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
                    textArea.setText(previousState);  // Установка предыдущего состояния текстовой области
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }
}