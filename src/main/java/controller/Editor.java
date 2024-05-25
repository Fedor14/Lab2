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

    private JTextArea textArea;
    private JFrame frame;
    private File openedFile = null;

    private UndoManager undoManager;

    private List<EditorObserver> observers = new ArrayList<>();

    private boolean isUpdating = false;

    private String previousState;

    @Autowired
    private LoggingAspect loggingAspect;

    private final SimpleMutex mutex = new SimpleMutex();

    private void acquireLock() throws InterruptedException {
        mutex.lock();
    }

    private void releaseLock() {
        mutex.unlock();
    }

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
        try {
            acquireLock();
            observers.add(observer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    private void notifyObservers() {
        try {
            acquireLock();
            String text = textArea.getText();
            for (EditorObserver observer : observers) {
                observer.update(text);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    public void setText(String text) {
        try {
            acquireLock();
            isUpdating = true;
            try {
                textArea.setText(text);
            } finally {
                isUpdating = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            releaseLock();
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

        // Use self-injected bean to call logging method
        this.loggingAspect.logEditorActions(actionCommand);

        System.out.println("Action performed: " + actionCommand);
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
                new Thread(() -> {
                    try {
                        acquireLock();
                        textArea.setText("");
                        openedFile = null;
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

                    // Чтение файла в отдельном потоке
                    new Thread(() -> {
                        try {
                            acquireLock();
                            openedFile = selectedFile;

                            BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                            StringBuilder content = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append("\n");
                            }
                            reader.close();

                            // Обновляем текстовое поле в главном потоке через Event Dispatch Thread
                            SwingUtilities.invokeLater(() -> textArea.setText(content.toString()));
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
                new Thread(() -> {
                    try {
                        acquireLock();
                        saveFile();
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

    class SaveAsFileHandler implements ActionHandler {
        private ActionHandler next;

        SaveAsFileHandler(ActionHandler next) {
            this.next = next;
        }

        public void handleRequest(String actionCommand) {
            if (actionCommand.equals("Save as")) {
                new Thread(() -> {
                    try {
                        acquireLock();
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
                new Thread(() -> {
                    try {
                        acquireLock();
                        frame.dispose();
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
                previousState = textArea.getText();
                SwingUtilities.invokeLater(() -> undoManager.undo()); // Выполнение в основном потоке Swing
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
                    textArea.setText(previousState);
                }
            } else {
                next.handleRequest(actionCommand);
            }
        }
    }
}