package com.jesta.data.notification;

import androidx.annotation.NonNull;

public enum TopicDescriptor {
    REQUEST_TODO_JESTA,
    USER_INBOX;

    @NonNull
    @Override
    public String toString() {
        return this.name();
    }

}
