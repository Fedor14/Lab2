package observer;

import controller.Editor;

public class TextEditorObserver implements EditorObserver {

    private Editor editor;

    public TextEditorObserver(Editor editor) {
        this.editor = editor; // Присваивание переданного экземпляра Editor полю editor
    }

    @Override
    public void update(String text) { // Реализация метода update интерфейса EditorObserver
        editor.setText(text); // Вызов метода setText экземпляра Editor с переданным текстом
    }
}
