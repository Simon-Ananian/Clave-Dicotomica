
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

//Clase que representa la tabla Hash
class TablaHash {
    private int tamaño;
    // Clase interna para representar especies y sus características
    class ClaveEspecie {
        String especie;
        ClavePreguntas [] preguntas;
        ClaveEspecie siguiente; // Manejo de colisiones (encadenamiento)
        public ClaveEspecie(String especie, ClavePreguntas[] preguntas) {
            this.especie = especie;
            this.preguntas = preguntas;
            this.siguiente = null;
        }
    }
    
    // Clase que interna representa la pregunta, con su valor booleano (Sí o No)
    class ClavePreguntas {
        String pregunta;
        boolean valor;

        public ClavePreguntas(String pregunta, boolean valor) {
            this.pregunta = pregunta;
            this.valor = valor;
        }
    }

    private ClaveEspecie[] tabla; // Arreglo para almacenar las especies y sus características

    //Constructor que se inicializa con la cantidad de especies del JSON
    public TablaHash(int tamaño) {
        this.tamaño = tamaño;
        this.tabla = new ClaveEspecie[tamaño]; //
    }

    // Función hash basada en la suma de los valores ASCII de los caracteres en la clave (nombre de la especie)
    private int hash(String clave) {
        int hash = 0;
        // Recorre cada carácter en la cadena de entrada 'clave'
        for (int i = 0; i < clave.length(); i++) {
            // Suma el valor ASCII del carácter actual al hash acumulativo
            hash += clave.charAt(i);
        }
        // Utiliza el operador módulo (residuo) para garantizar que el índice resultante esté dentro de los límites del arreglo 'tabla'
        return hash % tabla.length; 
    }

    // Método para insertar una especie y sus preguntas
    public void insertar(String especie, ClavePreguntas[] preguntas) {
        int indice = hash(especie); //Calcula el indice de la tabla
        ClaveEspecie nuevo = new ClaveEspecie(especie, preguntas);
        if (tabla[indice] == null) {
            tabla[indice] = nuevo; // Insertar directamente si no hay colisión
        } else {
            // Encadenamiento para manejar colisiones
            ClaveEspecie actual = tabla[indice];
            while (actual.siguiente != null) {
                actual = actual.siguiente;
            }
            actual.siguiente = nuevo;
        }
    }

    // Método para construir la Tabla Hash desde JSON
    public void construirDesdeJSON(JSONObject json, JSONArray especies) {
        for (int i = 0; i < especies.length(); i++) {
            JSONObject especieObj = especies.getJSONObject(i);
            String especie = especieObj.keys().next();
            JSONArray preguntas = especieObj.getJSONArray(especie);

            ClavePreguntas[] preguntasClave = new ClavePreguntas[preguntas.length()];
            for (int j = 0; j < preguntas.length(); j++) {
                JSONObject preguntaObj = preguntas.getJSONObject(j);
                String clave = preguntaObj.keys().next();
                boolean valor = preguntaObj.getBoolean(clave);
                preguntasClave[j] = new ClavePreguntas(clave, valor);
            }
            insertar(especie, preguntasClave); // Insertar en la tabla hash
        }
    }
    
    //Metodo para buscar la especie por Hash
    public void buscarEspecie(String nombreEspecie) {
        long inicio = System.nanoTime();
        int indice = hash(nombreEspecie); // Calcular el índice en la tabla hash
        ClaveEspecie actual = tabla[indice]; // Obtener la lista en el índice
        // Buscar la especie en la lista vinculada (manejo de colisiones)
        while (actual != null) {
            if (actual.especie.equals(nombreEspecie)) {
                StringBuilder mensaje = new StringBuilder("Especie encontrada: " + actual.especie + "\nPreguntas que llevaron a esta especie:\n");
                for (ClavePreguntas pregunta : actual.preguntas) {
                    mensaje.append(" - ¿").append(pregunta.pregunta).append("?: ").append(pregunta.valor ? "Sí" : "No").append("\n");
                }
                long fin = System.nanoTime();
                mensaje.append("\nTiempo de búsqueda por Hash: ").append((fin - inicio) / 1_000_000.0).append(" ms");
                JOptionPane.showMessageDialog(null, mensaje.toString(), "Búsqueda de Especie por Hash", JOptionPane.INFORMATION_MESSAGE);
                return; // Especie encontrada, salir del método
            }
            actual = actual.siguiente; // Continuar con el siguiente nodo en caso de colisión
        }
        // Si no se encuentra la especie
        JOptionPane.showMessageDialog(null, "Especie no encontrada o no hay suficientes preguntas para identificarla", "Búsqueda de Especie por Hash", JOptionPane.WARNING_MESSAGE);
    }   

}
