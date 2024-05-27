package controller;

// Обработчик для команды "New"
class NewFileHandler implements ActionHandler {
    private final Editor editor;
    private ActionHandler next;  // Ссылка на следующий обработчик в цепочке

    NewFileHandler(Editor editor, ActionHandler next) {
        this.editor = editor;
        this.next = next;  // Инициализация следующего обработчика
    }

    public void handleRequest(String actionCommand) {
        if (actionCommand.equals("New")) {
            new Thread(() -> {  // Создание нового потока для обработки команды
                try {
                    editor.readWriteLock.writeLock();  // Захват блокировки
                    editor.textArea.setText("");  // Очистка текстовой области
                    editor.openedFile = null;  // Сброс открытого файла
                } catch (InterruptedException ex) {
                    ex.printStackTrace();  // Обработка исключения
                } finally {
                    editor.readWriteLock.writeUnlock();  // Освобождение блокировки
                }
            }).start();  // Запуск потока
        } else {
            next.handleRequest(actionCommand);  // Передача команды следующему обработчику в цепочке
        }
    }
}
