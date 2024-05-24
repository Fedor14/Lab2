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

@Component
public class Editor implements ActionListener {

    private JTextArea textArea;
    private JFrame frame;
    private File openedFile = null;

    private UndoManager undoManager;

    private List<EditorObserver> observers = new ArrayList<>();

    private boolean isUpdating = false;

    private String previousState;

    @Autowired
    private LoggingAspect loggingAspect;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    @Autowired
    public Editor(EditorUI editorUI, UndoManager undoManager, LoggingAspect loggingAspect) {
        this.frame = editorUI.getFrame();
        this.textArea = new JTextArea();
        this.undoManager = undoManager;
        this.loggingAspect = loggingAspect;

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

        setActionCommands(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);
        addActionListeners(newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, closeMenuItem, forwardMenuItem, backMenuItem);

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        editMenu.add(forwardMenuItem);
        editMenu.add(backMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        frame.setJMenuBar(menuBar);
        frame.add(textArea);

        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers();
                }
            }

            public void removeUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers();
                }
            }

            public void changedUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    notifyObservers();
                }
            }
        });
        editorUI.display();
    }

    public void addObserver(EditorObserver observer) {
        writeLock.lock();
        try {
            observers.add(observer);
        } finally {
            writeLock.unlock();
        }
    }

    private void notifyObservers() {
        readLock.lock();
        try {
            String text = textArea.getText();
            for (EditorObserver observer : observers) {
                observer.update(text);
            }
        } finally {
            readLock.unlock();
        }
    }

    public void setText(String text) {
        writeLock.lock();
        try {
            isUpdating = true;
            try {
                textArea.setText(text);
            } finally {
                isUpdating = false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void setActionCommands(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.setActionCommand(item.getText());
        }
    }

    private void addActionListeners(JMenuItem... items) {
        for (JMenuItem item : items) {
            item.addActionListener(this);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        ActionHandler handlerChain = new NewFileHandler(new OpenFileHandler(new SaveFileHandler(new SaveAsFileHandler(new CloseHandler(new ForwardHandler(new BackHandler(null)))))));
        handlerChain.handleRequest(actionCommand);

        // Используем самовнедренный бин для вызова метода логирования
        this.loggingAspect.logEditorActions(actionCommand);

        System.out.println("Выполнено действие: " + actionCommand);
    }

    interface ActionHandler {
        void handleRequest(String actionCommand);
    }

    class NewFileHandler implements ActionHandler {
        private ActionHandler next;

        NewFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("New")) {
                writeLock.lock();
                try {
                    textArea.setText("");
                    openedFile = null;
                } finally {
                    writeLock.unlock();
                }
            } else {
                next.handleRequest(actionCommand);
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
                int returnValue = fileChooser.showOpenDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    writeLock.lock();
                    try {
                        openedFile = selectedFile;

                        BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        reader.close();

                        textArea.setText(content.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        writeLock.unlock();
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
                saveFile();
            } else {
                next.handleRequest(actionCommand);
            }
        }

        private void saveFile() {
            writeLock.lock();
            try {
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
            } finally {
                writeLock.unlock();
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
                writeLock.lock();
                try {
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
                } finally {
                    writeLock.unlock();
                }
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
                writeLock.lock();
                try {
                    frame.dispose();
                } finally {
                    writeLock.unlock();
                }
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
                if (undoManager.canUndo()) {
                    // Сохраняем текущее состояние перед отменой действия
                    previousState = textArea.getText();
                    undoManager.undo();
                }
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
                // Восстанавливаем сохраненное состояние при выполнении действия "Following"
                if (previousState != null) {
                    textArea.setText(previousState);
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }
}
