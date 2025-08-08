package com.example.nhlapp;

public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String error);
}
