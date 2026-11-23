package com.emanuelgb.sensores.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.emanuelgb.sensores.dao.SensorDAOImpl;
import com.emanuelgb.sensores.exception.IoTException;
import com.emanuelgb.sensores.model.SensorBase;
import com.emanuelgb.sensores.model.SensorColor;
import com.emanuelgb.sensores.model.SensorDistancia;
import com.emanuelgb.sensores.model.SensorHumedad;
import com.emanuelgb.sensores.model.SensorTemperatura;

import jakarta.servlet.http.HttpSession;

@Controller
public class IoTController {

    @Autowired
    private SensorDAOImpl dao;

    @GetMapping("/")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("usuario") != null) return "redirect:/dashboard";
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String username,
                                @RequestParam String password,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (dao.validarLogin(username, password)) {
            session.setAttribute("usuario", username);
            return "redirect:/dashboard";
        }
        ra.addFlashAttribute("error", "Usuario o contraseña incorrectos.");
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/";

        var sensores = dao.listarTodos();
        var bitacora = dao.obtenerBitacora();
        long activos = sensores.stream().filter(SensorBase::isActivo).count();

        model.addAttribute("sensores", sensores);
        model.addAttribute("bitacora", bitacora);
        model.addAttribute("totalSensores", sensores.size());
        model.addAttribute("sensoresActivos", activos);
        return "dashboard";
    }

    @PostMapping("/sensores/guardar")
    public String guardarSensor(@RequestParam String idSensor,
                                @RequestParam String nombre,
                                @RequestParam String tipo,
                                @RequestParam int puerto,
                                @RequestParam double umbralMax,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        try {
            SensorBase s = switch (tipo) {
                case "TEMPERATURA" -> new SensorTemperatura();
                case "COLOR" -> new SensorColor();
                case "DISTANCIA" -> new SensorDistancia();
                default -> new SensorHumedad();
            };
            s.setIdSensor(idSensor);
            s.setNombre(nombre);
            s.setPuerto(puerto);
            s.setUmbralMax(umbralMax);
            s.setActivo(true);
            dao.guardar(s);
            ra.addFlashAttribute("exito", "Sensor agregado correctamente a la red.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/sensores/editar")
    public String editarSensor(@RequestParam String idSensor,
                               @RequestParam String nombre,
                               @RequestParam int puerto,
                               @RequestParam double umbralMax,
                               @RequestParam(defaultValue = "false") boolean activo,
                               HttpSession session,
                               RedirectAttributes ra) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        try {
            SensorBase s = dao.buscarPorId(idSensor);
            s.setNombre(nombre);
            s.setPuerto(puerto);
            s.setUmbralMax(umbralMax);
            s.setActivo(activo);
            dao.actualizar(s);
            ra.addFlashAttribute("exito", "Parámetros de " + idSensor + " actualizados.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/sensores/eliminar/{id}")
    public String eliminarSensor(@PathVariable String id,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        if (session.getAttribute("usuario") == null) return "redirect:/";
        try {
            dao.eliminar(id);
            ra.addFlashAttribute("exito", "Sensor retirado de la red.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/monitorear")
    public String ejecutarMonitoreo(HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("usuario") == null) return "redirect:/";

        int alertas = 0;
        for (SensorBase sensor : dao.listarTodos()) {
            try {
                double lectura = sensor.obtenerLectura();
                dao.registrarBitacora(sensor.getIdSensor(), lectura, "NORMAL", "Lectura dentro de rango");
            } catch (IoTException e) {
                alertas++;
                String estado = e.getMessage().contains("Conexion") ? "ERROR_CONEXION" : "ALERTA_ANOMALA";
                dao.registrarBitacora(sensor.getIdSensor(), 0.0, estado, e.getMessage());
            }
        }
        if (alertas > 0) {
            ra.addFlashAttribute("error", "Ciclo finalizado con " + alertas + " excepción(es) registrada(s) en MySQL.");
        } else {
            ra.addFlashAttribute("exito", "Ciclo de lectura completado sin anomalías y guardado en BD.");
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/api/bitacora")
    @ResponseBody
    public ResponseEntity<List< Map < String, Object >>> obtenerBitacoraJson(HttpSession session) {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dao.obtenerBitacora());
    }

    @PostMapping("/api/lectura-fisica")
    @ResponseBody
    public ResponseEntity<String> guardarLecturaFisica(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(401).body("No autorizado");
        }
        String idSensor = payload.get("idSensor").toString();
        double valor = Double.parseDouble(payload.get("valor").toString());
        SensorBase sensor = dao.buscarPorId(idSensor);

        if (sensor == null) {
            return ResponseEntity.badRequest().body("Sensor no encontrado");
        }

        if (valor > sensor.getUmbralMax()) {
            dao.registrarBitacora(idSensor, valor, "ALERTA_ANOMALA", "LecturaAnomalaException (Hardware Real): Valor " + valor + " supera umbral");
            return ResponseEntity.ok("ALERTA");
        } else {
            dao.registrarBitacora(idSensor, valor, "NORMAL", "Lectura física recibida por puerto serie");
            return ResponseEntity.ok("NORMAL");
        }
    }
}