package controller;

public class ReadWriteLock {
    private int readers = 0; // Читатели
    private int writers = 0; // Писатели

    private boolean writerActive = false; // Наличие писателей

    // Блокировки для чтения
    public synchronized void readLock() throws InterruptedException {
        // Пока есть ждущие писатели или уже есть писатель, поток будет ждать
        while (writers > 0 || writerActive) {
            wait();
        }
        // Увеличиваем количество читателей
        readers++;
    }

    // Разблокировка для чтения
    public synchronized void readUnlock() {
        // Уменьшаем количество читателей
        readers--;
        // Если больше нет читателей, уведомляем все ожидающие потоки
        if (readers == 0) {
            notifyAll();
        }
    }

    // Метод блокировки для записи
    public synchronized void writeLock() throws InterruptedException {
        // Увеличиваем количество писателей
        writers++;
        // Пока есть читатели или работающий писатель, поток будет ждать
        while (readers > 0 || writerActive) {
            wait();
        }
        // Писатель работает
        writerActive = true;
        // Уменьшаем количество писателей, один из них уже работает
        writers--;
    }

    // Метод разблокировки для записи
    public synchronized void writeUnlock() {
        // Писатель закончил работу
        writerActive = false;
        // Уведомляем все ожидающие потоки
        notifyAll();
    }
}
