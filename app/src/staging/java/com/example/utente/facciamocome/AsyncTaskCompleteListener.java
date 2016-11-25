package com.example.utente.facciamocome;

/**
 * Created by utente on 21/05/2016.
 */
public interface AsyncTaskCompleteListener<T1, T> {
    public void onTaskComplete(T1 id, T result);
}
