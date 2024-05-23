package controller;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.event.*;
import java.io.*;

import aspect.LoggingAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ui.EditorUI;

@Component
public class Editor implements ActionListener {

    private JTextArea textArea;
    private JFrame frame;
    private File openedFile = null;

    private UndoManager undoManager;

    @Autowired
    private LoggingAspect loggingAspect;

    @Autowired
    public Editor(EditorUI editorUI, UndoManager undoManager) {
        this.frame = editorUI.getFrame();
        this.textArea = new JTextArea();
        this.undoManager = undoManager;

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
        editorUI.display();
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
                textArea.setText("");
                openedFile = null;
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
                    openedFile = selectedFile;

                    try {
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
