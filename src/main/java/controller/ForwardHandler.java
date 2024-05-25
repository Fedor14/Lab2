package controller;

import javax.swing.*;

class ForwardHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    ForwardHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Previous")) {
            editor.previousState = editor.textArea.getText();  // Сохранение предыдущего состояния текстовой области
            SwingUtilities.invokeLater(() -> editor.undoManager.undo());  // Выполнение операции отмены
        } else {
            next.handleRequest(actionCommand);
        }
    }
}
