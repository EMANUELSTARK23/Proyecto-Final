package com.emanuelgb.sensores.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.emanuelgb.sensores.model.SensorBase;
import com.emanuelgb.sensores.model.SensorColor;
import com.emanuelgb.sensores.model.SensorDistancia;
import com.emanuelgb.sensores.model.SensorHumedad;
import com.emanuelgb.sensores.model.SensorTemperatura;

@Repository
public class SensorDAOImpl implements GenericDAO<SensorBase, String> {

    @Autowired
    private DataSource dataSource;

    private SensorBase instanciarSensor(String tipo) {
        return switch (tipo) {
            case "TEMPERATURA" -> new SensorTemperatura();
            case "COLOR" -> new SensorColor();
            case "DISTANCIA" -> new SensorDistancia();
            default -> new SensorHumedad();
        };
    }

    @Override
    public List<SensorBase> listarTodos() {
        List<SensorBase> lista = new ArrayList<>();
        String sql = "SELECT * FROM sensores ORDER BY id_sensor ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                SensorBase s = instanciarSensor(rs.getString("tipo_sensor"));
                s.setIdSensor(rs.getString("id_sensor"));
                s.setNombre(rs.getString("nombre"));
                s.setPuerto(rs.getInt("puerto"));
                s.setUmbralMax(rs.getDouble("umbral_max"));
                s.setActivo(rs.getBoolean("activo"));
                lista.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }
        return lista;
    }

    @Override
    public SensorBase buscarPorId(String id) {
        return listarTodos().stream()
                .filter(s -> s.getIdSensor().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void guardar(SensorBase s) throws Exception {
        if (s.getIdSensor() == null || s.getIdSensor().trim().isEmpty() ||
            s.getNombre() == null || s.getNombre().trim().isEmpty()) {
            throw new Exception("Validación: El ID y el Nombre no pueden estar vacíos.");
        }
        String sql = "INSERT INTO sensores (id_sensor, nombre, tipo_sensor, puerto, umbral_max, activo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getIdSensor());
            ps.setString(2, s.getNombre());
            ps.setString(3, s.getTipo());
            ps.setInt(4, s.getPuerto());
            ps.setDouble(5, s.getUmbralMax());
            ps.setBoolean(6, s.isActivo());
            ps.executeUpdate();
        }
    }

    @Override
    public void actualizar(SensorBase s) throws Exception {
        String sql = "UPDATE sensores SET nombre=?, puerto=?, umbral_max=?, activo=? WHERE id_sensor=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNombre());
            ps.setInt(2, s.getPuerto());
            ps.setDouble(3, s.getUmbralMax());
            ps.setBoolean(4, s.isActivo());
            ps.setString(5, s.getIdSensor());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(String id) throws Exception {
        String sql = "DELETE FROM sensores WHERE id_sensor=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public boolean validarLogin(String user, String rawPass) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ? AND password_hash = SHA2(?, 256)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, rawPass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void registrarBitacora(String idSensor, double valor, String estado, String msg) {
        String sql = "INSERT INTO bitacora_lecturas (id_sensor, valor_registrado, estado, mensaje) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idSensor);
            ps.setDouble(2, valor);
            ps.setString(3, estado);
            ps.setString(4, msg);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List < Map < String, Object >> obtenerBitacora() {
        List < Map < String, Object >> logs = new ArrayList<>();
        String sql = "SELECT id_registro, id_sensor, valor_registrado, estado, mensaje, DATE_FORMAT(fecha_hora, '%Y-%m-%d %H:%i:%s') AS fecha_formateada FROM bitacora_lecturas ORDER BY fecha_hora DESC LIMIT 30";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id_registro"));
                row.put("sensor", rs.getString("id_sensor"));
                row.put("valor", rs.getDouble("valor_registrado"));
                row.put("estado", rs.getString("estado"));
                row.put("mensaje", rs.getString("mensaje"));
                row.put("fecha", rs.getString("fecha_formateada"));
                logs.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}