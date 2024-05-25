package controller;

class BackHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    BackHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Following")) {
            if (editor.previousState != null) {
                editor.textArea.setText(editor.previousState);  // Установка предыдущего состояния текстовой области
            }
        } else {
            next.handleRequest(actionCommand);
        }
    }
}
