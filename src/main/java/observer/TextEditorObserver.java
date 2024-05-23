package observer;

import controller.Editor;

public class TextEditorObserver implements EditorObserver {
    private Editor editor;

    public TextEditorObserver(Editor editor) {
        this.editor = editor;
    }

    @Override
    public void update(String text) {
        editor.setText(text);
    }
}
