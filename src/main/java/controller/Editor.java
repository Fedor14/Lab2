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

    public JTextArea textArea;  // Текстовая область для ввода текста
    public JFrame frame;  // Основное окно приложения
    public File openedFile = null;  // Текущий открытый файл, если есть

    public UndoManager undoManager;  // Менеджер отмены действий для текстовой области

    public List<EditorObserver> observers = new ArrayList<>();  // Список наблюдателей, которые будут уведомлены об изменениях текста

    public boolean isUpdating = false;  // Флаг для предотвращения рекурсивных вызовов при обновлении текста

    public String previousState;  // Предыдущее состояние текста, используется для функции отмены

    @Autowired
    public LoggingAspect loggingAspect;  // Логирования действий

    public final SimpleMutex mutex = new SimpleMutex();  // Мьютекс для синхронизации доступа

    public void acquireLock() throws InterruptedException {  // Метод для получения блокировки
        mutex.lock();
    }

    public void releaseLock() {  // Метод для освобождения блокировки
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
    public void notifyObservers() {
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
    public void setActionCommands(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.setActionCommand(item.getText());
        }
    }

    // Метод для добавления слушателей действий для пунктов меню
    public void addActionListeners(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.addActionListener(this);
        }
    }

    // Метод, вызываемый при выполнении действия (нажатии пункта меню)
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();  // Получение команды действия

        // Создание цепочки ответственности для обработки команд действий
        ActionHandler handlerChain = new NewFileHandler(this, new OpenFileHandler(this, new SaveFileHandler(this, new SaveAsFileHandler(this, new CloseHandler(this, new ForwardHandler(this, new BackHandler(this, null)))))));
        handlerChain.handleRequest(actionCommand);  // Обработка команды действия

        // Вызов метода логирования действий через внедренный аспект
        this.loggingAspect.logEditorActions(actionCommand);

        System.out.println("Action performed: " + actionCommand);
    }

}