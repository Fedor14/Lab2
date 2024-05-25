package controller;

public class SimpleMutex {
    private boolean locked = false;  // Заблокирован ли мьютекс

    public synchronized void lock() throws InterruptedException {
        // Ждёт пока мьютекс не будет доступен
        while (locked) {
            wait();  // Если мьютекс заблокирован, ждем пока не будет вызван notify()
        }
        locked = true;  // Блок
    }

    // Разблокировка мьютекса
    public synchronized void unlock() {
        locked = false;  // Снятие блокировки
        notify();  // Уведомляет один из потоков, который ждет блокировку, о том, что мьютекс теперь доступен
    }
}

