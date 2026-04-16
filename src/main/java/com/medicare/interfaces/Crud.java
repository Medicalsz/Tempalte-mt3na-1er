package com.medicare.interfaces;

import java.util.List;

public interface Crud<T> {
    void add(T t);
    void update(T t);
    void delete(int id);
    List<T> getAll();
}