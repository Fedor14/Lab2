package controller;

class CloseHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;

    CloseHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("Close")) {
            new Thread(() -> {  // Создание нового потока
                try {
                    editor.readWriteLock.writeLock();
                    editor.frame.dispose();  // Закрытие окна приложения
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    editor.readWriteLock.writeUnlock();
                }
            }).start();
        } else {
            next.handleRequest(actionCommand);
        }
    }
}
