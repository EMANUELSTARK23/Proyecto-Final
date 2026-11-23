package com.emanuelgb.sensores.dao;

import java.util.List;

public interface GenericDAO<T, ID> {
    List<T> listarTodos();
    T buscarPorId(ID id);
    void guardar(T entidad) throws Exception;
    void actualizar(T entidad) throws Exception;
    void eliminar(ID id) throws Exception;
}