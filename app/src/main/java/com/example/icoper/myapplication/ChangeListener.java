package com.example.icoper.myapplication;

import java.util.Observable;

public class ChangeListener extends Observable {
    private boolean focusState;

    public void setSomeVariable(boolean someVariable) {
            this.focusState = someVariable;

        setChanged();
        notifyObservers(focusState);
    }

    public boolean getSomeVariable() {
        return focusState;
    }
}
